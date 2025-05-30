package com.textifyer;

import android.os.Bundle;
import android.os.AsyncTask;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.textifyer.databinding.ActivityMainBinding;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ProgressBar progressBar;
    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip";
    private static final String MODEL_DIR_NAME = "vosk-model";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressBar = findViewById(R.id.progressBar);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        if (!isModelDownloaded()) {
            startModelDownload();
        } else {
            initializeModel();
        }

        // Rest der bestehenden MainActivity-Initialisierung
    }

    private boolean isModelDownloaded() {
        File modelDir = new File(getFilesDir(), MODEL_DIR_NAME);
        return modelDir.exists() && containsModelFiles(modelDir);
    }

    private boolean containsModelFiles(File dir) {
        String[] requiredFiles = {"am", "conf", "graph", "ivector"};
        for (String file : requiredFiles) {
            if (!new File(dir, file).exists()) return false;
        }
        return true;
    }

    private void startModelDownload() {
        new ModelDownloadTask().execute(MODEL_URL);
    }

    private class ModelDownloadTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Modell wird heruntergeladen...", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                File modelDir = new File(getFilesDir(), MODEL_DIR_NAME);
                if (!modelDir.exists()) modelDir.mkdirs();

                InputStream input = connection.getInputStream();
                File zipFile = new File(modelDir, "model.zip");
                FileOutputStream output = new FileOutputStream(zipFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.close();
                input.close();
                unzipFile(zipFile, modelDir);
                zipFile.delete();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

            private void unzipFile(File zipFile, File targetDir) throws Exception {
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry;
                String modelSubdir = null;

                // Erste Pass: Finde den Hauptmodellordner
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (zipEntry.getName().contains("am") && !zipEntry.isDirectory()) {
                        modelSubdir = zipEntry.getName().substring(0, zipEntry.getName().indexOf("am"));
                        break;
                    }
                }
                zis.close();

                // Zweite Pass: Extrahiere Dateien mit Pfadkorrektur
                zis = new ZipInputStream(new FileInputStream(zipFile));
                while ((zipEntry = zis.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    if (modelSubdir != null && entryName.startsWith(modelSubdir)) {
                        entryName = entryName.substring(modelSubdir.length());
                    }

                    File newFile = new File(targetDir, entryName);

                    if (zipEntry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        newFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                }

                zis.close();
                zipFile.delete();

                // Validiere die extrahierten Dateien
                if (!validateModelFiles(targetDir)) {
                    throw new Exception("Ungültige Modellstruktur");
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("model_path", targetDir.getAbsolutePath());
                editor.apply();
            }

            private boolean validateModelFiles(File dir) {
                String[] requiredFiles = {"am", "conf", "graph", "ivector"};
                for (String file : requiredFiles) {
                    if (!new File(dir, file).exists()) {
                        Log.e("Unzip", "Fehlende Datei: " + file);
                        return false;
                    }
                }
                return true;
            }



        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            if (success) {
                saveModelPath();
                initializeModel();
                Toast.makeText(MainActivity.this, "Modell bereit", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Download fehlgeschlagen", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveModelPath() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("model_path", new File(getFilesDir(), MODEL_DIR_NAME).getAbsolutePath());
        editor.apply();
    }

    private void initializeModel() {
        // Hier könnte die Modelinitialisierung für andere Komponenten erfolgen
    }

    // Rest der MainActivity-Methoden
}
