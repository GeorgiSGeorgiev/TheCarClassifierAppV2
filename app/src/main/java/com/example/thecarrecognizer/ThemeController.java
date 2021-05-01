package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Color;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentContainerView;

public class ThemeController {
    public static final int LIGHT = 0;
    public static final int DARK = 1;

    public static int chosenTheme = LIGHT;

    public static void setChosenTheme(String theme) {
        switch (theme) {
            case "Light":
                chosenTheme = LIGHT;
                break;
            case "Dark":
                chosenTheme = DARK;
                break;
        }
    }
}