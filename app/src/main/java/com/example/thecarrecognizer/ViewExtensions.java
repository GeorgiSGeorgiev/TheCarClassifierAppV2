package com.example.thecarrecognizer;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

/**
 * Static methods which "extend" the View class.
 */
public class ViewExtensions {
    /**
     * Changes the button background color and the button text button.
     * @param button The button we are changing.
     * @param backgroundColor The new background color of the button.
     * @param textColor The new text color of the button.
     */
    public static void ChangeButtonColor(Button button, int backgroundColor, int textColor) {
        button.setBackgroundColor(backgroundColor);
        button.setTextColor(textColor);
    }
}
