package com.textifyer;

public class LanguageItem {
    private final String languageCode;
    private final String languageName;
    private final int flagResId;

    public LanguageItem(String languageCode, String languageName, int flagResId) {
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.flagResId = flagResId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getLanguageName() {
        return languageName;
    }

    public int getFlagResId() {
        return flagResId;
    }
}
