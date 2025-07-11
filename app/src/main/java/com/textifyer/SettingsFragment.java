package com.textifyer;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.textifyer.language.LanguageAdapter;
import com.textifyer.language.LanguageItem;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "app_prefs";
    private static final String THEME_KEY = "theme";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = requireActivity().getSharedPreferences(PREFS_NAME, 0);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        ImageButton backbtn = view.findViewById(R.id.button_back);

        // Load saved theme preference
        int savedTheme = prefs.getInt(THEME_KEY, R.id.radio_system);
        radioGroup.check(savedTheme);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(THEME_KEY, checkedId);
            editor.apply();
            // Optionally, you can apply the theme immediately
            applyTheme(checkedId);
        });

        backbtn.setOnClickListener(view1 -> getActivity().onBackPressed());

        Spinner languageSpinner = view.findViewById(R.id.language_spinner);

        // Sprache laden
        String currentLang = prefs.getString("lang", "en");

        List<LanguageItem> languages = Arrays.asList(
                new LanguageItem("de", "Deutsch", R.drawable.flag_germany),
                new LanguageItem("en", "English", R.drawable.flag_uk),
                new LanguageItem("es","Español", R.drawable.flag_es),
                new LanguageItem("fr","Français", R.drawable.flag_fr)
        );

        LanguageAdapter adapter = new LanguageAdapter(requireContext(), languages);
        languageSpinner.setAdapter(adapter);

        // Aktuelle Sprache setzen
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).getLanguageCode().equals(currentLang)) {
                languageSpinner.setSelection(i);
                break;
            }
        }

        // Sprachwechsel
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = languages.get(position).getLanguageCode();
                if (!selectedLang.equals(currentLang)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("lang", selectedLang);
                    editor.apply();
                    setLocale(selectedLang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        return view;
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

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
        requireActivity().recreate();  // Activity neu starten für Sprachübernahme
    }

}
