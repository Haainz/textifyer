package com.textifyer.audio;

import android.content.Context;
import org.vosk.Model;
import java.io.IOException;

public class VoskModelManager {
    private static Model currentModel;
    private static String currentLang;

    // This method is key
    public static Model getModel(Context context, String langCode) throws IOException {
        if (currentModel != null && langCode.equals(currentLang)) {
            return currentModel; // Return cached model if language hasn't changed
        }

        // If model exists but language is different, release old one
        if (currentModel != null) {
            // Consider if you need to explicitly close/release resources of the old model
            // currentModel.close(); // Vosk models might have a close or release method
            // As per your code, it seems you re-assign,
            // and the Vosk library might handle old model cleanup
            // or you might need to handle it if Model is a heavy resource.
            // Your current code re-creates it, which is fine if Model() handles this.
        }

        currentLang = langCode;
        // Construct the model path based on the language code
        // Ensure your model files in assets are named like "model-en", "model-de", "model-fr"
        String modelPath = "model-" + langCode;
    //    currentModel = new Model(context.getAssets(), modelPath);
        return currentModel;
    }

    public static void releaseModel() {
        if (currentModel != null) {
            // currentModel.close(); // If applicable for Vosk Model
            currentModel = null;
            currentLang = null;
        }
    }

    // Optional: Add a method to get the currently selected language
    public static String getCurrentLang() {
        return currentLang;
    }
}

//