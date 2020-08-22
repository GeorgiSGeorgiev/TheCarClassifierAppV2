package com.example.thecarrecognizer;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

public class ViewExtensions {
    public static void ChangeButtonColor(Button button, int backgroundColor, int textColor) {
        button.setBackgroundColor(backgroundColor);
        button.setTextColor(textColor);
    }
}
