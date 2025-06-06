package com.textifyer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
}
