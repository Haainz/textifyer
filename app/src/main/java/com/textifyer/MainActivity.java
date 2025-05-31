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
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
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

    private ActivityMainBinding binding;
    private static final String MODEL_DIR_NAME = "vosk-model";
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        binding.settingsbtn.setOnClickListener(v -> {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                NavDestination currentDestination = navHostFragment.getNavController().getCurrentDestination();
                if (currentDestination != null && currentDestination.getId() != R.id.SecondFragment) {
                    navHostFragment.getNavController().navigate(R.id.action_FirstFragment_to_SecondFragment);
                }
            }
        });
    }
}
