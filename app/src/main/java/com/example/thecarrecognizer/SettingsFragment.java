package com.example.thecarrecognizer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        ListPreference themePreference = findPreference("theme");
        ListPreference modePreference = findPreference("mode");
        this.setListPreferenceSummary(themePreference);
        this.setListPreferenceSummary(modePreference);

        EditTextPreference ipPreference = findPreference("server_IP");
        EditTextPreference portPreference = findPreference("server_port");
        this.setTextPreferenceSummary(ipPreference);
        this.setTextPreferenceSummary(portPreference);
        this.setIntOnlyTextPreference(portPreference);
    }

    private void setTextPreferenceSummary(EditTextPreference inputPreference) {
        if (inputPreference != null) {
            inputPreference
                    .setSummaryProvider((Preference.SummaryProvider<EditTextPreference>)
                            preference -> {
                                String text = preference.getText();
                                if (TextUtils.isEmpty(text)){
                                    return "Not set";
                                }
                                return text;
                            });
        }
    }

    private void setListPreferenceSummary(ListPreference inputPreference) {
        if (inputPreference != null) {
            inputPreference
                    .setSummaryProvider((Preference.SummaryProvider<ListPreference>)
                            preference -> {
                                String text = preference.getValue();
                                if (TextUtils.isEmpty(text)){
                                    return "Not set";
                                }
                                return text;
                            });
        }
    }

    private void setIntOnlyTextPreference(EditTextPreference textPreference) {
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }

    // listener that searches for value changes of concrete application setting
    // TODO: call static methods which change the theme of the whole app from here?
    SharedPreferences.OnSharedPreferenceChangeListener listener =
            (sharedPreferences, key) -> {
                if (key.equals("theme")) {
                    String result = sharedPreferences.getString(key, "");
                    if (result.equals("Light")) {
                        ThemeController.chosenTheme = ThemeController.LIGHT;
                        MainSettingsActivity.SetViewColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.White, null));
                    }
                    else {
                        ThemeController.chosenTheme = ThemeController.DARK;
                        MainSettingsActivity.SetViewColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.CyberBlack, null));
                    }
                }
            };

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
