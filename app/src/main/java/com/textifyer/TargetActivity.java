package com.textifyer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.RecognitionListener;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class TargetActivity extends AppCompatActivity implements RecognitionListener {

    private File opusFile;
    private TextView textOutput;
    private Model model;
    private StringBuilder liveText = new StringBuilder();
    private MediaPlayer mediaPlayer;
    private SpeechStreamService speechStreamService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String modelPath = prefs.getString("model_path", "");

        textOutput = findViewById(R.id.textOutput);
        Button btnTranscribe = findViewById(R.id.btnTranscribe);
        handleIntent(getIntent());

        btnTranscribe.setOnClickListener(v -> startTranscription());

        initModel(); // Model initialisieren
    }

    private void initModel() {
        StorageService.unpack(this, "model-de", "model-de",
                (model) -> {
                    this.model = model;
                    Log.d("Model", "Model successfully loaded");
                },
                (exception) -> {
                    Log.e("Model", "Failed to unpack the model: " + exception.getMessage());
                    runOnUiThread(() -> Toast.makeText(this, "Model loading failed", Toast.LENGTH_LONG).show());
                });
    }

    private void startTranscription() {
        textOutput.setText("");
        liveText.setLength(0);

        new Thread(() -> {
            try {
                if (model == null) {
                    Log.e("Transcription", "Model is not initialized");
                    return;
                }

                File wavFile = convertOpusToWav(opusFile); // Konvertiere die Opus-Datei in WAV
                runOnUiThread(() -> playAudio(wavFile));
                transcribeAudio(wavFile); // Transkribiere die WAV-Datei

            } catch (Exception e) {
                Log.e("Transcription", "Fehler: ", e);
                runOnUiThread(() -> textOutput.setText("Fehler: " + e.getMessage()));
            }
        }).start();
    }

    private void playAudio(File audioFile) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e("MediaPlayer", "Error: " + what + ", " + extra);
                    return true;
                });
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d("MediaPlayer", "Duration: " + mp.getDuration() + "ms");
                mp.start();
            });

        } catch (IOException e) {
            Log.e("AudioPlay", "Playback error", e);
            runOnUiThread(() -> Toast.makeText(this, "Playback failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        }
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) &&
                intent.getType() != null &&
                intent.getType().startsWith("audio/")) {

            Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (audioUri != null) {
                opusFile = new File(getCacheDir(), "shared.opus");
                saveFileFromUri(audioUri, opusFile);
            }
        }
    }

    private void saveFileFromUri(Uri uri, File target) {
        try (InputStream input = getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(target)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File convertOpusToWav(File opusFile) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(opusFile.getPath());

        int trackIndex = selectAudioTrack(extractor);
        if (trackIndex < 0) {
            throw new Exception("No audio track found");
        }

        MediaFormat format = extractor.getTrackFormat(trackIndex);
        File wavFile = new File(getCacheDir(), "converted_" + System.currentTimeMillis() + ".wav");

        // Setze die Sample-Rate auf 16000 Hz, falls notwendig
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (sampleRate != 16000) {
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);
            Log.d("Audio", "Sample rate adjusted to 16000 Hz");
        }

        decodeAudioData(extractor, trackIndex, wavFile, format);

        // Debug: Dateigröße prüfen
        Log.d("Audio", "WAV file size: " + wavFile.length() + " bytes");

        extractor.release(); // Ressourcen freigeben
        return wavFile;
    }


    private int selectAudioTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    private void writeWavHeader(MediaFormat format, FileOutputStream stream, long dataSize) throws Exception {
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int bitsPerSample = 16; // Standardmäßig 16 Bit für PCM

        byte[] header = new byte[44];
        System.arraycopy("RIFF".getBytes(), 0, header, 0, 4);
        writeInt((int) (36 + dataSize), header, 4); // Platzhalter
        System.arraycopy("WAVE".getBytes(), 0, header, 8, 4);
        System.arraycopy("fmt ".getBytes(), 0, header, 12, 4);
        writeInt(16, header, 16); // Größe der fmt-Sektion
        writeShort(1, header, 20); // PCM-Format
        writeShort(channelCount, header, 22); // Anzahl der Kanäle
        writeInt(sampleRate, header, 24); // Sample-Rate
        writeInt(sampleRate * channelCount * bitsPerSample / 8, header, 28); // Byte pro Sekunde
        writeShort(channelCount * bitsPerSample / 8, header, 32); // Blockausrichtung
        writeShort(bitsPerSample, header, 34); // Bits pro Sample
        System.arraycopy("data".getBytes(), 0, header, 36, 4);
        writeInt((int) dataSize, header, 40); // Größe der Daten

        stream.write(header);
    }

    private void writeShort(int value, byte[] array, int offset) {
        array[offset] = (byte) (value & 0xff);
        array[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    private void decodeAudioData(MediaExtractor extractor, int trackIndex, File wavFile, MediaFormat format)
            throws Exception {

        try (FileOutputStream wavStream = new FileOutputStream(wavFile)) {
            // Schreiben eines vorläufigen Headers
            writeWavHeader(format, wavStream, 0); // 0 als Platzhalter

            MediaCodec codec = MediaCodec.createDecoderByType(
                    extractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME)
            );

            codec.configure(format, null, null, 0);
            codec.start();

            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            long totalDataSize = 0;

            extractor.selectTrack(trackIndex);

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufferIndex = codec.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inputBufferIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                    inputBufferIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            sawInputEOS = true;
                        } else {
                            codec.queueInputBuffer(
                                    inputBufferIndex, 0, sampleSize,
                                    extractor.getSampleTime(), 0
                            );
                            extractor.advance();
                        }
                    }
                }

                int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputBufferIndex >= 0) {
                    ByteBuffer buffer = outputBuffers[outputBufferIndex];
                    byte[] chunk = new byte[info.size];
                    buffer.get(chunk);

                    // Hier wird die Audio-Daten um das Vierfache beschleunigt
                    // Nur jeden sechsten Sample schreiben
                    for (int i = 0; i < chunk.length; i += 6) {
                        wavStream.write(chunk, i, 2); // Schreibe nur 2 Bytes (16 Bit) für jedes sechste Sample
                    }
                    totalDataSize += chunk.length / 6 * 2; // Aktualisiere die Gesamtgröße

                    codec.releaseOutputBuffer(outputBufferIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                }
            }

            // Header mit tatsächlicher Datenlänge aktualisieren
            updateWavHeader(wavFile, format, totalDataSize);

            codec.stop();
            codec.release();
            extractor.release();
        }
    }

    private void writeInt(int value, byte[] array, int offset) {
        array[offset] = (byte) (value & 0xff);
        array[offset + 1] = (byte) ((value >> 8) & 0xff);
        array[offset + 2] = (byte) ((value >> 16) & 0xff);
        array[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private void updateWavHeader(File wavFile, MediaFormat format, long dataSize) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            raf.seek(4);
            raf.writeInt(Integer.reverseBytes((int)(dataSize + 36)));

            raf.seek(40);
            raf.writeInt(Integer.reverseBytes((int) dataSize));

            Log.d("WAV", "Updated header with data size: " + dataSize);
        }
    }

    private void transcribeAudio(File wavFile) {
        try {
            InputStream ais = new FileInputStream(wavFile);

            // Überprüfen, ob der InputStream Daten enthält
            if (ais.available() <= 0) {
                Log.e("Transcription", "InputStream is empty");
                runOnUiThread(() -> textOutput.setText("Error: InputStream is empty"));
                return;
            }

            // Überprüfen der Sample-Rate
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(wavFile.getPath());
            int trackIndex = selectAudioTrack(extractor);
            if (trackIndex < 0) {
                Log.e("Transcription", "No audio track found");
                runOnUiThread(() -> textOutput.setText("Error: No audio track found"));
                return;
            }

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if (sampleRate != 16000) {
                Log.e("Transcription", "Sample rate is not 16000 Hz: " + sampleRate);
                runOnUiThread(() -> textOutput.setText("Error: Sample rate is not 16000 Hz"));
                extractor.release();
                return;
            }

            // Wenn alles in Ordnung ist, fahre mit der Transkription fort
            if (ais.skip(44) != 44) throw new IOException("File too short");

            Recognizer rec = new Recognizer(model, 16000.0f);
            rec.setMaxAlternatives(10);
            rec.setWords(true);

            speechStreamService = new SpeechStreamService(rec, ais, 16000);
            speechStreamService.start(this);

            extractor.release(); // Ressourcen freigeben
        } catch (IOException e) {
            Log.e("Transcription", "Error: ", e);
            runOnUiThread(() -> textOutput.setText("Error: " + e.getMessage()));
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        try {
            JSONObject partial = new JSONObject(hypothesis);
            if (partial.has("partial")) {
                String text = partial.getString("partial");
                runOnUiThread(() -> textOutput.setText(liveText.toString() + text));
            }
        } catch (JSONException e) {
            Log.e("JSON", "Error parsing partial", e);
        }
    }

    @Override
    public void onResult(String hypothesis) {
        Log.d("RecognitionResult", "Hypothesis: " + hypothesis); // Logge die Hypothese
        try {
            JSONObject result = new JSONObject(hypothesis);
            if (result.has("alternatives")) {
                JSONArray alternatives = result.getJSONArray("alternatives");
                for (int i = 0; i < alternatives.length(); i++) {
                    JSONObject alternative = alternatives.getJSONObject(i);
                    Log.d("RecognitionResult", "Alternative " + i + ": " + alternative.toString());
                }
            }
        } catch (JSONException e) {
            Log.e("JSON", "Error parsing result", e);
        }
        appendResult(hypothesis);
    }



    @Override
    public void onFinalResult(String hypothesis) {
        appendResult(hypothesis);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    private void appendResult(String json) {
        try {
            JSONObject result = new JSONObject(json);
            if (result.has("text")) {
                String text = result.getString("text");
                if (!text.isEmpty()) {
                    runOnUiThread(() -> {
                        liveText.append(text).append("\n");
                        textOutput.setText(liveText.toString());
                    });
                }
            }
        } catch (JSONException e) {
            Log.e("JSON", "Error parsing result", e);
        }
    }

    @Override
    public void onError(Exception e) {
        runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onTimeout() {
        runOnUiThread(() -> Toast.makeText(this, "Timeout", Toast.LENGTH_SHORT).show());
    }
}
