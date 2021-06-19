package com.example.thecarrecognizer;

import android.Manifest;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.*;
import java.util.List;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

// Created by: Georgi S. Georgiev

// Note 1: this whole project will be referred as "the application"
// Note 2: the main program which does the evaluation will be referred as "the core"

public class MainActivity extends AppCompatActivity implements PhotoUtilityActivityInterface {
    // result store point so the result can be showed more than once on user request
    String evaluationResult;
    String bestEvaluationResult;
    List<String> loadedLabels = null;
    SharedPreferences sharedAppPref; // the shared settings of the application

    // The main and only buttons of this activity
    Button selectPhotoButton;
    Button evalButton;
    Button showResultsButton;

    static ConstraintLayout mainLayout; // the layout of the class
    // there is only one main activity active

    // The image which will be displayed on the application after selection
    ImageView mainImageView;

    // items from the "Select Photo" dialog
    private static final String takePhoto = "Take Photo";
    private static final String chooseFromGallery = "Choose from Gallery";
    private static final String backStr = "Back";

    String currentPhotoPath = "";
    Bitmap currentPhotoBitmap; // the bitmap of the photo to be shown to the user

    // global application settings
    public static String serverIP;
    public static int portNumber;

    // activity listener for taking a photo from the gallery
    ActivityResultLauncher<Intent> chooseFromGalleryLauncher;
    // activity listener for taking a photo from the camera
    ActivityResultLauncher<Intent> takePhotoLauncher;

    /**
     * This method is called automatically on application start and serves as a main activity
     * initialization point.
     * @param savedInstanceState A Bundle that restores the activity to its last saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //<editor-fold desc="Main application initializations">

        // Run the original on Create constructor from the parent class AppCompatActivity.
        // Makes the default initializations that the application needs.
        super.onCreate(savedInstanceState);
        // Set the main view of the activity.
        // Contains the different buttons, labels, etc. of the activity.
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout); // needed to change the background color

        // Buttons init:
        selectPhotoButton = findViewById(R.id.selectPhotoButton);
        evalButton = findViewById(R.id.evalButton);
        showResultsButton = findViewById(R.id.showResultsButton);
        showResultsButton.setEnabled(false);

        // the view where the selected photo is shown to the user
        mainImageView = findViewById(R.id.mainImageView);
        // set the default image to be shown on application start
        this.currentPhotoBitmap = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.porsche_911_gts);

        // get saved cached data and restore the settings to the last available state
        sharedAppPref = getDefaultSharedPreferences(this);
        this.updateAppSettings();

        // Create a new activity result listener which gets image from the phone Gallery.
        // The lambda expression represents the onActivityResult listener.
        chooseFromGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri selectedPhotoUri = data.getData();
                            decodeAndShowPhotoFromUri(selectedPhotoUri);
                            // after the image has been changed the show results button is disabled
                            showResultsButton.setEnabled(false);
                        }
                    }
                });

        // Create a new activity result listener which takes photo from the camera.
        // The lambda expression represents the onActivityResult listener.
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        this.currentPhotoBitmap = ImageBuilder.decodeAndShowPhoto(
                                currentPhotoPath, mainImageView
                        ); // take the photo and then show it to the user via the ImageView
                        // after a new image has been taken the show results button is disabled
                        showResultsButton.setEnabled(false);
                    }
                });

        // handle received by sharing photo
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleReceivedImage(intent); // handle single image receive
            }
        }

        //</editor-fold>
    }

    void handleReceivedImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            decodeAndShowPhotoFromUri(imageUri);
        }
    }

    /**
     * Creates an image selection dialog. Allows to take a new photo or to choose an existing one.
     * @param view The main view where the dialog will be created.
     */
    public void selectPhoto(View view) {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        int themeID = getDialogThemeID();
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this,
                themeID);
        DialogInterface.OnClickListener dialogOnClickL = (dialogInterface, i) -> {
            // different options user can choose
            switch (options[i]) {
                case takePhoto:
                    handleTakePhotoIntent();
                    break;
                case chooseFromGallery:
                    Intent intent1 = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    chooseFromGalleryLauncher.launch(intent1);
                    break;
                case backStr:
                    dialogInterface.dismiss();
                    break;
            }
        };
        // set the dialog color according to the chosen application theme
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            alertDiaBuilder.setItems(options, dialogOnClickL);
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            ArrayAdapter<String> arrayAdapter = ThemeController.getCustomColoredListItemsAdapter(
                    MainActivity.this, options, getColor(R.color.White));
            alertDiaBuilder.setAdapter(arrayAdapter, dialogOnClickL);
        }

        alertDiaBuilder.show();
    }

    /**
     * Finds, decodes a photo from its Uri, shows it on the main image view and then
     * saves it as a bitmap which can be processed further in other methods.
     * @param photoURI The Uri address of the photo.
     */
    public void decodeAndShowPhotoFromUri(Uri photoURI) {
        mainImageView.setImageURI(photoURI);
        this.currentPhotoBitmap = ((BitmapDrawable) mainImageView.getDrawable()).getBitmap();
    }

    // get the dialog style ID directly from the resources
    private int getDialogThemeID() {
        int themeID = 0;
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            themeID = R.style.LightDialogTheme;
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            themeID = R.style.DarkDialogTheme;
        }
        return themeID;
    }

    // Main entry point of the photo taking.
    public void handleTakePhotoIntent() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // check and ask for the necessary permissions
            if (ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                // You can use the API that requires the permission.
                this.takePhoto();
            } else {
                // Ask for the permission directly. The result is registered and handled.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        } else {
            this.showAlertDialog("No camera detected", true);
        }
    }

    // camera permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted-> {
                if (isGranted) {
                    this.takePhoto();
                } else {
                    String message = "Camera access request denied.\n" +
                            "The application won't be able to launch the Camera and take a photo.";
                    this.showAlertDialog(message, true);
                }
            });

    // The actual method that starts the camera and saves the result to the memory
    private void takePhoto() {
        // Intent describes the action that we want to do. So create one.
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File resultPhotoFile = null;
        try {
            // try to save the photo
            resultPhotoFile = ImageBuilder.createEmptyPhotoFile(MainActivity.this);
            this.currentPhotoPath = resultPhotoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // continue the operation if the file with the photo was created successfully
        if (resultPhotoFile != null) {
            Uri resultPhotoURI = FileProvider.getUriForFile(this,
                    "com.example.android.file_provider", resultPhotoFile);
            // save the actual photo to the photo file via an intent
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, resultPhotoURI);
            try {
                takePhotoLauncher.launch(takePhotoIntent);
            } catch (Exception e) {
                System.out.println("Failed to start the camera intent.");
            }
        }
    }

    /**
     * Method that handles the photo evaluation process. Asks to send the photo for remote
     * evaluation or starts the direct evaluation via the neural network in the application.
     * @param view The view which is connected with calls this method.
     */
    public void startEvaluationProcess(View view) {
        String selectedMode = sharedAppPref.getString("mode",
                getString(R.string.default_mode));
        String[] modes = getResources().getStringArray(R.array.mode_values);

        if (selectedMode.equals(modes[0])) { // Direct evaluation
            this.createEvalImgConfirmDialog();
        }
        else if (selectedMode.equals(modes[1])) {
            this.createSendImgConfirmDialog();
        }
    }

    /**
     * Asks the user whether to continue the direct image evaluation.
     * Upon confirmation proceeds with the evaluation otherwise cancels the operation.
     */
    public void createEvalImgConfirmDialog() {
        int themeID = getDialogThemeID();
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this,
                themeID);
        String alertMessage = "The selected image will be evaluated. Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> handleModel())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    /**
     * Asks the user to confirm whether to proceed with the photo sending to the server for
     * distant evaluation. Upon confirmation the method tries to connect with the server.
     * The connection settings can be changed via the application settings menu.
     */
    public void createSendImgConfirmDialog() {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        String alertMessage = "The selected image will be sent to the server for evaluation. " +
                              "Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> handleServerCommunication())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    // evaluates the neural network model directly and saves the evaluation result
    private void handleModel() {
        MLModel model = new MLModel(MainActivity.this);
        ModelResultPair[] results = model.evalDirectly(this.currentPhotoBitmap);
        loadedLabels = model.getLoadedLabels();
        bestEvaluationResult = results[0].getLabel();
        evaluationResult = model.convertPairArrayToString(results);
        this.showResultDialog(this.evaluationResult);
        this.showResultsButton.setEnabled(evaluationResult != null && !evaluationResult.equals(""));
    }

    // tries to connect with the distant server which is supposed to make the evaluation and return
    // the final result to the application. The connection won't be successful if the wrong settings
    // were added or of the server is offline.
    public void handleServerCommunication() {
        OkHTTP3Controller okHTTP3Controller = new OkHTTP3Controller(MainActivity.this,
                serverIP, portNumber);
        // If the controller IP address is private that means the server and the mobile device must
        // both be connected to it. Otherwise the data transfer won't work.
        // The default port 5000 is a TCP/UDP port and is often used for such communications.
        try {
            okHTTP3Controller.connectServer(this.currentPhotoBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Connection error..." + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Gets the result data and writes them on the selected view in a special dialog.
     * @param view The view which calls the method.
     */
    public void getDirectResult(View view) {
        this.showResultDialog(this.evaluationResult);
    }

    /**
     * Basic alert dialog construction and showing tool.
     * @param message The actual message to be displayed on the dialog.
     * @param cancelable Boolean which defines whether the Alert dialog will be cancelable or not.
     */
    public void showAlertDialog(String message, Boolean cancelable) {
        int themeID = getDialogThemeID();
        if (message != null && !message.equals("")) {
            new AlertDialog.Builder(MainActivity.this, themeID)
                    .setMessage(message) // write the result data on the dialog panel
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .setCancelable(cancelable)
                    .show(); // show the dialog until the 'OK' button is clicked
        }
    }

    /**
     * Result alert dialog construction and showing tool. Result dialog can browse the web as well.
     * The URLs can be changed from the "strings" resource file. The URL is chosen according to
     * the categorization result.
     * @param message The actual message to be displayed on the dialog.
     */
    public void showResultDialog(String message) {
        int themeID = getDialogThemeID();
        if (message != null && !message.equals("")) {
            Uri bestSearchUri = null;
            Uri dealershipUri = null;
            // get the right URIs which provide the Internet search part of the result output.
            if (bestEvaluationResult.equals(loadedLabels.get(0))){
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_full_size_coupe_sedan_cars_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.full_size_coupe_sedan_cars_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(1))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_hatchbacks_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.hatchbacks_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(2))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_muscle_cars_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.muscle_car_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(3))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_pickups_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.pickup_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(4))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_sports_cars_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.sports_cars_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(5))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_suv_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.suv_dealerships_link));
            } else if (bestEvaluationResult.equals(loadedLabels.get(7))) {
                bestSearchUri = Uri.parse(getResources()
                        .getString(R.string.best_van_link));
                dealershipUri = Uri.parse(getResources().
                        getString(R.string.van_dealerships_link));
            }

            Intent bestSearchBrowserIntent = new Intent(Intent.ACTION_VIEW, bestSearchUri);
            Intent closestDealershipBrowserIntent = new Intent(Intent.ACTION_VIEW, dealershipUri);

            // activate the dialog
            if (!bestSearchUri.equals(null) && !dealershipUri.equals(null)) {
                new AlertDialog.Builder(MainActivity.this, themeID)
                        .setMessage(message) // write the result data on the dialog panel
                        .setNegativeButton("Buy a Car",
                                (dialog, id) -> startActivity(closestDealershipBrowserIntent))
                        .setNeutralButton("Find Best Cars",
                                (dialog, id) -> startActivity(bestSearchBrowserIntent))
                        .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                        .setCancelable(false)
                        .show(); // show the dialog until the 'OK' button is clicked
            }
            else showAlertDialog(message, false);
        }
    }

    /**
     * Called directly after the options menu has been created. Used to initialize the different
     * menu items. From the application the menu can be activated by clicking on the tipple dots
     * button in the upper right corner.
     * @param menu The actual menu which has been created.
     * @return Returns true for the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu_item, menu);
        return true;
    }

    /**
     * Called after the user selects a menu item. Used to process the item selection.
     * @param item The menu item which was selected by the user.
     * @return True to process the item selection.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, MainSettingsActivity.class);
            startActivity(intent); // start the settings menu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets the saved settings from the application cache and restores them.
     * Uses shared Application Preferences. If setting was corrupted then loads its default value
     * to prevent the application from crashing.
     */
    public void updateAppSettings() {
        // Restore the application theme.
        String theme = sharedAppPref.getString("theme", getString(R.string.default_theme));
        ThemeController.setChosenTheme(theme);

        this.updateBackground();

        serverIP = sharedAppPref.getString("server_IP", getString(R.string.default_ip));
        String port = sharedAppPref.getString("server_port", getString(R.string.default_port));
        portNumber = Integer.parseInt(port); // set the port number
    }

    /**
     * Changes the background color of the actual active MainActivity layout.
     * @param color The new color to be set as a background.
     */
    public static void setBackgroundColor(int color) {
        mainLayout.setBackgroundColor(color);
    }

    //
    private void updateBackground() {
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            setBackgroundColor(getColor(R.color.White));
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            setBackgroundColor(getColor(R.color.CyberBlack));
        }
    }
}