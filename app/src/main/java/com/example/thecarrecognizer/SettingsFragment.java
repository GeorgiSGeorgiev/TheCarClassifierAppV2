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
// Created by: Georgi S. Georgiev
// Main information source: https://developer.android.com/guide

/**
 * Class representing the settings menu back-end. Here is done the whole manipulation with
 * the application cache.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Show the settings preferences (the settings items).
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        // Get the current theme setting.
        ListPreference themePreference = findPreference("theme");
        // Get the current mode setting.
        ListPreference modePreference = findPreference("mode");

        // Set the theme settings description according to its current state.
        this.setListPreferenceSummary(themePreference);
        // Set the mode settings description according to its current state.
        this.setListPreferenceSummary(modePreference);

        // Get the currently inserted IP.
        EditTextPreference ipPreference = findPreference("server_IP");
        // Get the currently inserted port.
        EditTextPreference portPreference = findPreference("server_port");
        // Write the current IP as a setting description.
        this.setTextPreferenceSummary(ipPreference);
        // Write the current port as a setting description.
        this.setTextPreferenceSummary(portPreference);
        // Set the port setting as an Integer-only.
        this.setIntOnlyTextPreference(portPreference);
    }

    // Update the state text of the setting. Changes according to the currently selected option.
    private void setTextPreferenceSummary(EditTextPreference inputPreference) {
        if (inputPreference != null) {
            // Summary provider allows to set the summary (description) text of the setting.
            inputPreference
                    .setSummaryProvider((Preference.SummaryProvider<EditTextPreference>)
                            preference -> {
                                String text = preference.getText(); // get the text which was set
                                if (TextUtils.isEmpty(text)){
                                    return "Not set";
                                }
                                return text;
                            });
        }
    }

    // Update the state text of the setting. Changes according to the currently selected option.
    private void setListPreferenceSummary(ListPreference inputPreference) {
        if (inputPreference != null) {
            // Summary provider allows to set the summary (description) text of the setting.
            inputPreference
                    .setSummaryProvider((Preference.SummaryProvider<ListPreference>)
                            preference -> {
                                String text = preference.getValue(); // get the value which was set
                                if (TextUtils.isEmpty(text)){
                                    return "Not set";
                                }
                                return text;
                            });
        }
    }

    // change the preference input type to number only
    private void setIntOnlyTextPreference(EditTextPreference textPreference) {
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }

    // listener that searches for value changes of concrete application setting
    SharedPreferences.OnSharedPreferenceChangeListener listener =
            (sharedPreferences, key) -> {
                // Determine the setting changed by its key.
                switch (key) {
                    case "theme":
                        // Get the setting value.
                        String result = sharedPreferences.getString(key, "");
                        // Change the theme according to the value we got.
                        if (result.equals("Light")) {
                            // Update the static variable that memorizes the theme setting.
                            ThemeController.chosenTheme = ThemeController.LIGHT;
                            // Set the application settings activity background color.
                            MainSettingsActivity.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.White, null));
                            // Set the main application activity background color.
                            MainActivity.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.White, null));
                        } else {
                            // Update the static variable that memorizes the theme setting.
                            ThemeController.chosenTheme = ThemeController.DARK;
                            // Set the application settings activity background color.
                            MainSettingsActivity.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.CyberBlack, null));
                            // Set the main application activity background color.
                            MainActivity.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.CyberBlack, null));
                        }
                        break;
                    case "grayscaleBoosting":
                        // Get the evaluation mode from the settings and set it in the application.
                        MLModel.grayscaleMode = sharedPreferences.getBoolean(key, false);
                        break;
                    case "server_IP":
                        // Get the server IP from the settings and set it in the application.
                        MainActivity.serverIP = sharedPreferences.getString(key, "");
                        break;
                    case "server_port":
                        // Get the port number from the settings and set it in the application.
                        MainActivity.portNumber =
                                Integer.parseInt(sharedPreferences.getString(key, ""));
                        break;
                }

            };

    // If the settings menu is active once again, then register a new preference change listener
    // because the settings may be changed by the user.
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    // If the settings menu was paused, then unregister the preference change listener
    // because the settings are not supposed to be changed while the user is not in
    // the settings menu.
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
