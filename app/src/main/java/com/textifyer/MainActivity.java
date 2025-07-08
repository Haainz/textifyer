package com.textifyer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import com.textifyer.databinding.ActivityMainBinding;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // 1. Unterstützte Sprachen
        String[] supportedLangs = {"de", "en", "es"};

        // 2. Gespeicherte Sprache holen
        String savedLang = prefs.getString("lang", null); // null = nicht gesetzt

        if (savedLang == null) {
            // 3. Sprache nicht gesetzt → Systemsprache ermitteln
            String systemLang = Locale.getDefault().getLanguage();

            boolean isSupported = false;
            for (String lang : supportedLangs) {
                if (lang.equals(systemLang)) {
                    isSupported = true;
                    break;
                }
            }

            String finalLang = isSupported ? systemLang : "en"; // Fallback zu Englisch
            prefs.edit().putString("lang", finalLang).apply();
            setAppLocale(finalLang);
        } else {
            // 4. Sprache war gesetzt → übernehmen
            setAppLocale(savedLang);
        }

        super.onCreate(savedInstanceState);

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

        if (getIntent().getBooleanExtra("navigate_to_home", false)) {
            NavController navController1 = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController1.popBackStack(R.id.FirstFragment, false); // gehe zum Home-Fragment
        }

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

    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
