package com.example.thecarrecognizer;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;

public class MainSettingsActivity extends AppCompatActivity {
    // There is always just one active activity so this is safe
    // even for multiple settings activities.
    static FragmentContainerView mainView;

    public MainSettingsActivity() {
        super(R.layout.fragment_container_view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainView = findViewById(R.id.fragmentContainerView);
        this.updateBackground();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            // Contains two different methods of creating a new fragment that contains the
            // settings menu buttons and options. You can choose either of them according to your
            // programming style and preferences. Both methods give the same result.
            getSupportFragmentManager().beginTransaction()
                    // First method: replace removes the old fragment and
                    // creates a new one with the same settings every time.
                    // If used, the "if" (savedInstanceState == null) constraint may not be needed.
                    //.replace(R.id.fragment_container_view, new SettingsFragment())
                    .setReorderingAllowed(true) // operation optimizations
                    // Second method: Directly creates a new fragment. In that case
                    // the "if" (savedInstanceState == null) constraint is needed.
                    .add(R.id.fragmentContainerView, SettingsFragment.class, null)
                    .commit();
        }
        // this.getView().setBackgroundColor(Color.WHITE);
    }

    /**
     * Called when the actionbar "return" (the back arrow) button has been clicked.
     * @param item The actionbar menu item that has been clicked.
     * @return  True to activate the return operation.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // close the settings activity
            return true; // return to the main activity
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public static void setBackgroundColor(int color) {
        mainView.setBackgroundColor(color);
     }

    private void updateBackground() {
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            setBackgroundColor(getColor(R.color.White));
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            setBackgroundColor(getColor(R.color.CyberBlack));
        }
    }
}