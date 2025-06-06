package com.textifyer;

import static androidx.core.graphics.drawable.DrawableCompat.applyTheme;

import android.os.Bundle;
import android.os.AsyncTask;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedTheme = prefs.getInt("theme", R.id.radio_system);
        applyTheme(savedTheme);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();

        // Beobachte das aktuelle Fragment
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.SecondFragment) {
                // Im Settings-Fragment -> Home-Icon anzeigen
                binding.settingsbtn.setImageResource(R.drawable.icon_home);
            } else {
                // In allen anderen Fragmenten -> Settings-Icon anzeigen
                binding.settingsbtn.setImageResource(R.drawable.icon_settings);
            }
        });

        binding.settingsbtn.setOnClickListener(v -> {
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null) {
                if (currentDestination.getId() == R.id.SecondFragment) {
                    // Von Settings zurück zu Home
                    navController.navigate(R.id.action_SecondFragment_to_FirstFragment);
                } else if (currentDestination.getId() == R.id.FirstFragment) {
                    // Von Home zu Settings
                    navController.navigate(R.id.action_FirstFragment_to_SecondFragment);
                }
            }
        });
    }


    private void applyTheme(int checkedId) {
        if (checkedId == R.id.radio_light) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (checkedId == R.id.radio_dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
