package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageView;

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

    public static void updateTheme(Context context, Button button) {
        switch (chosenTheme) {
            case DARK:
                ViewExtensions.ChangeButtonColor(button, context.getColor(R.color.LightGrey),
                        Color.BLACK);
                break;
            case LIGHT:
                ViewExtensions.ChangeButtonColor(button, context.getColor(R.color.LightBlack),
                        Color.WHITE);
                break;
        }
    }

    public static void updateTheme(Context context, ImageView backgroundView) {
        switch (chosenTheme) {
            case DARK:
                backgroundView.setBackgroundColor(context.getColor(R.color.CyberBlack));
                break;
            case LIGHT:
                backgroundView.setBackgroundColor(Color.WHITE);
                break;
        }
    }

    public static void updateTheme(Context context, ConstraintLayout constraintLayout) {
        switch (chosenTheme) {
            case DARK:
                constraintLayout.setBackgroundColor(context.getColor(R.color.CyberBlack));
                break;
            case LIGHT:
                constraintLayout.setBackgroundColor(Color.WHITE);
                break;
        }
    }

    public static void updateTheme(Context context, FragmentContainerView fragmentContainer) {
        switch (chosenTheme) {
            case DARK:
                fragmentContainer.setBackgroundColor(context.getColor(R.color.CyberBlack));
                break;
            case LIGHT:
                fragmentContainer.setBackgroundColor(Color.WHITE);
                break;
        }
    }
}