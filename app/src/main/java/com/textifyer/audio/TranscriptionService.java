package com.textifyer.audio;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.textifyer.R;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;

import java.io.*;
import java.nio.ByteBuffer;

public class TranscriptionService {

    private SpeechStreamService speechStreamService;
    // wird in convertToWav gesetzt und in startVoskTranscription genutzt
    private int wavSampleRate = 16000;

    public interface TranscriptionCallback {
        void onProgress(int progress); // 0–100
        void onStatusChanged(String status);
        void onFinished(File wavFile);
        void onError(Exception e);
    }

    public void startTranscription(Context context,
                                   File inputFile,
                                   Model model,
                                   RecognitionListener listener,
                                   TranscriptionCallback callback) {
        new Thread(() -> {
            try {
                callback.onStatusChanged(context.getString(R.string.statusconverting));
                File wavFile = convertToWav(context, inputFile, callback);

                callback.onStatusChanged(context.getString(R.string.statustranscribe));
                startVoskTranscription(wavFile, model, listener);

                callback.onFinished(wavFile);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void stop() {
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        }
    }

    private void startVoskTranscription(File wavFile,
                                        Model model,
                                        RecognitionListener listener) throws IOException {
        InputStream ais = new FileInputStream(wavFile);
        if (ais.skip(44) != 44)
            throw new IOException("invalid WAV-Header");

        // Recognizer mit dynamischer Sample-Rate initialisieren
        Recognizer recognizer = new Recognizer(model, wavSampleRate);
        recognizer.setWords(true);

        speechStreamService = new SpeechStreamService(recognizer, ais, wavSampleRate);
        speechStreamService.start(listener);
    }

    private File convertToWav(Context context,
                              File inputFile,
                              TranscriptionCallback callback) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(inputFile.getPath());

        int trackIndex = selectAudioTrack(extractor);
        if (trackIndex < 0) throw new Exception("No audio-track found");

        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int sampleRate   = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        File outputWav = new File(
                context.getCacheDir(),
                "converted_" + System.currentTimeMillis() + ".wav"
        );

        decodeToWav(
                context,
                extractor,
                trackIndex,
                format,
                inputFile.length(),
                outputWav,
                sampleRate,
                channelCount,
                callback
        );

        extractor.release();
        return outputWav;
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

    private void decodeToWav(Context context,
                             MediaExtractor extractor,
                             int trackIndex,
                             MediaFormat format,
                             long originalFileSize,
                             File outputWav,
                             int sampleRate,
                             int channelCount,
                             TranscriptionCallback callback) throws Exception {

        extractor.selectTrack(trackIndex);

        MediaCodec decoder = MediaCodec.createDecoderByType(
                format.getString(MediaFormat.KEY_MIME)
        );
        decoder.configure(format, null, null, 0);
        decoder.start();

        ByteBuffer[] inputBuffers  = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo info  = new MediaCodec.BufferInfo();

        String mime = format.getString(MediaFormat.KEY_MIME);
        boolean isM4a = mime != null && mime.contains("mp4a");
        boolean isMp3 = mime != null && mime.equals("audio/mpeg");
        boolean isAac = mime != null && mime.equals("audio/mp4a-latm");

        // 1) Interval bestimmen, 2) wavSampleRate berechnen
        int interval;
        if (isMp3) {
            // 4× schneller: nur jedes 4. Sample schreiben
            wavSampleRate = 16000;
            interval = 2 * 4; // 2 Bytes pro Sample × 4
        } else if(isAac) {
            wavSampleRate = 16000;
            interval = 2 * 3; // 2 Bytes pro Sample × 3
        } else {
            // ursprüngliche Reduktion: m4a→6× langsamer, andere (opus etc.)→3× langsamer
            interval = isM4a ? 12 : 6;
            // effektive Sample-Rate = originalRate / (interval/2)
            wavSampleRate = sampleRate / (interval / 2);
        }

        try (FileOutputStream wavStream = new FileOutputStream(outputWav)) {
            // WAV-Header mit berechneter Sample-Rate
            writeWavHeader(
                    wavStream,
                    wavSampleRate,
                    channelCount,
                    channelCount,
                    0
            );

            boolean sawInputEOS  = false;
            boolean sawOutputEOS = false;
            long totalWritten    = 0;

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                    inIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            sawInputEOS = true;
                        } else {
                            decoder.queueInputBuffer(
                                    inIndex, 0, sampleSize,
                                    extractor.getSampleTime(), 0
                            );
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outIndex >= 0) {
                    ByteBuffer outBuffer = outputBuffers[outIndex];
                    byte[] chunk = new byte[info.size];
                    outBuffer.get(chunk);

                    // für MP3 wie oben, für alle anderen nur alle interval-Bytes 2 schreiben
                    for (int i = 0; i + 1 < chunk.length; i += interval) {
                        wavStream.write(chunk, i, 2);
                        totalWritten += 2;
                    }

                    int progress = (int) ((totalWritten * 100) / originalFileSize);
                    callback.onProgress(progress);
                    if (progress >= 99) callback.onStatusChanged(context.getString(R.string.statusanalysing));

                    decoder.releaseOutputBuffer(outIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                }
            }

            updateWavHeader(outputWav, totalWritten);
        }

        decoder.stop();
        decoder.release();
    }

    private void writeWavHeader(OutputStream out,
                                int sampleRate,
                                int channels,
                                int channelCount,
                                long dataSize) throws IOException {
        byte[] header = new byte[44];
        int bitsPerSample = 16;
        long byteRate = sampleRate * channels * bitsPerSample / 8;

        System.arraycopy("RIFF".getBytes(), 0, header, 0, 4);
        writeInt((int) (36 + dataSize), header, 4);
        System.arraycopy("WAVE".getBytes(), 0, header, 8, 4);
        System.arraycopy("fmt ".getBytes(), 0, header, 12, 4);
        writeInt(16, header, 16);
        writeShort(1, header, 20);
        writeShort(channelCount, header, 22);
        writeInt(sampleRate, header, 24);
        writeInt((int) byteRate, header, 28);
        writeShort((int) (channels * bitsPerSample / 8), header, 32);
        writeShort(bitsPerSample, header, 34);
        System.arraycopy("data".getBytes(), 0, header, 36, 4);
        writeInt((int) dataSize, header, 40);

        out.write(header);
    }

    private void updateWavHeader(File wavFile, long dataSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            raf.seek(4);
            raf.writeInt(Integer.reverseBytes((int) (36 + dataSize)));
            raf.seek(40);
            raf.writeInt(Integer.reverseBytes((int) dataSize));
        }
    }

    private void writeInt(int value, byte[] array, int offset) {
        array[offset]     = (byte) (value & 0xff);
        array[offset + 1] = (byte) ((value >> 8) & 0xff);
        array[offset + 2] = (byte) ((value >> 16) & 0xff);
        array[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private void writeShort(int value, byte[] array, int offset) {
        array[offset]     = (byte) (value & 0xff);
        array[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
}