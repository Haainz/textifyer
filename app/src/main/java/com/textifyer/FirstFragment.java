package com.textifyer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.textifyer.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private static final String TAG = "FirstFragment";

    private FragmentFirstBinding binding;

    // Permission string depending on Android version
    private String requiredPermission;

    // Launcher for permission request
    private ActivityResultLauncher<String> permissionLauncher;

    // Launcher for picking audio content
    private ActivityResultLauncher<String> audioPickerLauncher;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Determine which permission to use based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            requiredPermission = Manifest.permission.READ_MEDIA_AUDIO;
            Log.d(TAG, "Using permission: READ_MEDIA_AUDIO");
        } else {
            requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            Log.d(TAG, "Using permission: READ_EXTERNAL_STORAGE");
        }

        // Register permission request launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Permission granted.");
                        openAudioPicker();
                    } else {
                        Log.d(TAG, "Permission denied.");
                        handlePermissionDenied();
                    }
                });

        // Register audio picker launcher
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "Audio selected: " + uri.toString());
                        openTargetActivityWithUri(uri);
                    } else {
                        Log.d(TAG, "No audio selected");
                        Toast.makeText(requireContext(), "No audio selected.", Toast.LENGTH_SHORT).show();
                    }
                });

        Button selectBtn = view.findViewById(R.id.button_first);
        selectBtn.setOnClickListener(v -> checkAndRequestPermission());
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), requiredPermission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission already granted.");
            openAudioPicker();
        } else {
            Log.d(TAG, "Requesting permission: " + requiredPermission);
            permissionLauncher.launch(requiredPermission);
        }
    }

    private void openAudioPicker() {
        // Using ACTION_GET_CONTENT via ActivityResult launcher, filtered for audio/* mimetype
        audioPickerLauncher.launch("audio/*");
    }

    private void openTargetActivityWithUri(Uri audioUri) {
        Intent intent = new Intent(requireActivity(), TargetActivity.class);
        intent.putExtra(Intent.EXTRA_STREAM, audioUri);
        requireActivity().startActivity(intent);
    }

    private void handlePermissionDenied() {
        // Check if user selected 'Don't ask again'
        if (!shouldShowRequestPermissionRationale(requiredPermission)) {
            Toast.makeText(requireContext(),
                    "Permission denied permanently. Please enable permission in Settings.",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(),
                    "Permission denied. You need to grant permission to select audio files.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}