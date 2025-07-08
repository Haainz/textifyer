package com.textifyer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.textifyer.audio.AudioPlayer;
import com.textifyer.audio.TranscriptionService;
import com.textifyer.utils.FileUtils;
import com.textifyer.utils.LocaleHelper;

import org.vosk.Model;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.IOException;

public class TargetActivity extends AppCompatActivity {

    private TextView textOutput;
    private Button btnTranscribe;
    private ImageButton btnShare, btnCopy, btnPlayPause;
    private RelativeLayout btnLayout;
    private TextView dataname, progressText;
    private ProgressBar progressBar;

    private File audioFile;
    private Model model;

    private final StringBuilder liveText = new StringBuilder();

    private AudioPlayer audioPlayer;
    private TranscriptionService transcriptionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Spracheinstellung
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lang = prefs.getString("lang", "de");
        LocaleHelper.setAppLocale(this, lang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        initViews();
        initListeners();
        handleIntent(getIntent());

        audioPlayer = new AudioPlayer();
        transcriptionService = new TranscriptionService();

        initModel();
    }

    private void initViews() {
        textOutput = findViewById(R.id.textOutput);
        btnTranscribe = findViewById(R.id.btnTranscribe);
        btnTranscribe.setVisibility(GONE);

        btnLayout = findViewById(R.id.buttonLayout);
        btnLayout.setVisibility(GONE);

        btnShare = findViewById(R.id.btn_share);
        btnCopy = findViewById(R.id.btn_copy);
        btnPlayPause = findViewById(R.id.btn_playpause);
        int greenColor = getResources().getColor(R.color.green_500);
        btnShare.setColorFilter(greenColor);
        btnCopy.setColorFilter(greenColor);


        dataname = findViewById(R.id.datatxt);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
    }

    private void initListeners() {
        btnShare.setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intent, "Teilen"));
            }
            // Icon ändern
            btnShare.setImageResource(R.drawable.icon_trump);
            btnShare.clearColorFilter();

            // 5 Sekunden später zurücksetzen
            btnShare.postDelayed(() -> {
                btnShare.setImageResource(R.drawable.icon_share); // dein ursprüngliches Icon
                btnShare.setColorFilter(getResources().getColor(R.color.green_500));
            }, 3000);
        });

        btnCopy.setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Text", text);
                clipboard.setPrimaryClip(clip);
            }
            // Icon ändern
            btnCopy.setImageResource(R.drawable.icon_trump);
            btnCopy.clearColorFilter();

            // 5 Sekunden später zurücksetzen
            btnCopy.postDelayed(() -> {
                btnCopy.setImageResource(R.drawable.icon_copy); // dein ursprüngliches Icon
                btnCopy.setColorFilter(getResources().getColor(R.color.green_500));
            }, 3000);
        });


        btnPlayPause.setOnClickListener(v -> {
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                btnPlayPause.setImageResource(R.drawable.icon_play);
            } else {
                audioPlayer.resume();
                btnPlayPause.setImageResource(R.drawable.icon_pause);
            }
        });

        btnTranscribe.setOnClickListener(v -> {
            btnTranscribe.setVisibility(GONE);
            btnLayout.setVisibility(GONE);
            startTranscription();
        });

        findViewById(R.id.homebtn).setOnClickListener(v -> {
            Intent intent = new Intent(TargetActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void handleIntent(Intent intent) {
        Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (audioUri != null) {
            String filename = FileUtils.getFileNameFromUri(getContentResolver(), audioUri);
            audioFile = new File(getCacheDir(), filename);
            try {
                FileUtils.copyUriToFile(getContentResolver(), audioUri, audioFile);
                dataname.setText(filename);

                // AAC-Warnung anzeigen
                if (filename.toLowerCase().endsWith(".aac")) {
                    progressText.setText(getString(R.string.errorsmayoccur));
                    progressText.setVisibility(VISIBLE);
                }

            } catch (IOException e) {
                Toast.makeText(this, "File could not be loaded", Toast.LENGTH_LONG).show();
                Log.e("TargetActivity", "Error when copying", e);
            }
        } else {
            Toast.makeText(this, "No File received", Toast.LENGTH_SHORT).show();
            Log.e("TargetActivity", "Intent URI missing");
        }
    }

    private void initModel() {
        StorageService.unpack(this, "model-de", "model-de",
                (model) -> {
                    this.model = model;
                    btnTranscribe.setVisibility(VISIBLE);
                },
                (exception) -> {
                    Log.e("Model", "Loading error: " + exception.getMessage());
                    Toast.makeText(this, "Model could not be loaded", Toast.LENGTH_LONG).show();
                });
    }

    private void startTranscription() {
        textOutput.setText("");
        liveText.setLength(0);
        progressBar.setVisibility(VISIBLE);
        progressText.setVisibility(VISIBLE);

        transcriptionService.startTranscription(
                this,
                audioFile,
                model,
                new org.vosk.android.RecognitionListener() {
                    @Override
                    public void onPartialResult(String hypothesis) {
                        String partial = extractText(hypothesis, "partial");
                        runOnUiThread(() -> {
                            textOutput.setText(liveText.toString() + partial);
                            // Sobald wir den ersten Text bekommen, Fortschrittsanzeige ausblenden
                            if (!partial.isEmpty()) {
                                progressBar.setVisibility(GONE);
                            }
                        });
                    }

                    @Override
                    public void onResult(String hypothesis) {
                        String result = extractText(hypothesis, "text");
                        if (result != null && !result.isEmpty()) {
                            liveText.append(result).append("\n");
                        }
                    }

                    @Override
                    public void onFinalResult(String hypothesis) {
                        String result = extractText(hypothesis, "text");
                        if (result != null && !result.isEmpty()) {
                            liveText.append(result).append("\n");
                            runOnUiThread(() -> textOutput.setText(liveText.toString()));
                        }
                        runOnUiThread(() -> {
                            progressText.setVisibility(GONE);
                            btnLayout.setVisibility(VISIBLE);
                            btnTranscribe.setVisibility(VISIBLE);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast.makeText(TargetActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onTimeout() {
                        runOnUiThread(() -> Toast.makeText(TargetActivity.this, "Timeout", Toast.LENGTH_SHORT).show());
                    }
                },
                new TranscriptionService.TranscriptionCallback() {
                    @Override
                    public void onProgress(int progress) {
                        runOnUiThread(() -> progressBar.setProgress(progress));
                    }

                    @Override
                    public void onStatusChanged(String status) {
                        runOnUiThread(() -> progressText.setText(status));
                    }

                    @Override
                    public void onFinished(File wavFile) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(GONE);
                            audioPlayer.play(audioFile, false,
                                    () -> btnPlayPause.setImageResource(R.drawable.icon_play),
                                    () -> btnPlayPause.setImageResource(R.drawable.icon_play),
                                    () -> Toast.makeText(TargetActivity.this, "Playback failed.", Toast.LENGTH_SHORT).show());
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(GONE);
                            progressText.setVisibility(GONE);
                            textOutput.setText("Error: " + e.getMessage());
                        });
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayer.release();
        transcriptionService.stop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        recreate();
    }

    private String extractText(String json, String key) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            return obj.optString(key, "");
        } catch (Exception e) {
            return "";
        }
    }
}
