package com.example.thecarrecognizer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
// Created by: Georgi S. Georgiev
// Main information source: https://developer.android.com/guide

/**
 * The main controller of the application themes.
 */
public class ThemeController {
    public static final int LIGHT = 0;
    public static final int DARK = 1;

    public static int chosenTheme = LIGHT;

    /**
     * Set the application theme. At this moment can be only Light or Dark.
     * @param theme The theme name. Can be set to: "Light" or "Dark".
     */
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

    /**
     * Create a custom-colored list of items.
     * @param context The context where the list is located.
     * @param options The full list of options which are located in the ListView.
     * @param textColor The new text color of the items.
     */
    public static ArrayAdapter<String> getCustomColoredListItemsAdapter(
            Context context, String[] options, int textColor) {
        return new ArrayAdapter<String>
                (context, android.R.layout.simple_list_item_1, options){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item on the given position directly from the list.
                // To get the item call the original implementation of this function which is
                // inside the ArrayAdapter class.
                View itemView = super.getView(position, convertView, parent);
                // Get the TextView of the selected Item. "text1" defines a normal-sized text label.
                TextView itemTextView = itemView.findViewById(android.R.id.text1);

                // Set the color of the text inside the selected list item.
                itemTextView.setTextColor(textColor);

                // Return the modified item view.
                return itemView;
            }
        };
    }
}