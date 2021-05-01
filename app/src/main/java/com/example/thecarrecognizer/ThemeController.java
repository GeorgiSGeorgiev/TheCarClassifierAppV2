package com.example.thecarrecognizer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

    public static ArrayAdapter<String> getCustomColoredListItemsAdapter(
            Context context, String[] options, int textColor) {
        return new ArrayAdapter<String>
                (context, android.R.layout.simple_list_item_1, options){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View parentView = super.getView(position, convertView, parent);

                // Initialize a TextView for each item of the list.
                TextView textView = parentView.findViewById(android.R.id.text1);

                // Set the text color of the list items.
                textView.setTextColor(textColor);

                // Return the modified parent view.
                return parentView;
            }
        };
    }
}