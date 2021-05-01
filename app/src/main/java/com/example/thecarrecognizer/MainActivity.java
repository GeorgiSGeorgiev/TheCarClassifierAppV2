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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.*;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

// Created by: Georgi S. Georgiev

// Note 1: this whole project will be referred as "the application"
// Note 2: the main program which does the evaluation will be referred as "the core"

public final class MainActivity extends AppCompatActivity {
    String evaluationResult;
    SharedPreferences sharedAppPref; // the shared settings of the application

    // Main buttons
    Button selectPhotoButton;
    Button evalButton;
    Button showResultsButton;

    static ConstraintLayout mainLayout; // the layout of the class
    // no other instances of this class are meant to exist than the default one

    // The image which will be displayed on the application after selection
    ImageView mainImageView;

    private static final String takePhoto = "Take Photo";
    private static final String chooseFromGallery = "Choose from Gallery";
    private static final String backStr = "Back";

    String currentPhotoPath = "";
    Bitmap currentPhotoBitmap;

    // global application settings
    public static String serverIP;
    public static int portNumber;

    // custom request codes used by the application
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    // activity listener for taking a photo from the gallery
    ActivityResultLauncher<Intent> chooseFromGalleryLauncher;
    // activity listener for taking a photo from the camera
    ActivityResultLauncher<Intent> takePhotoLauncher;

    // This method is called automatically on application start.
    // It is the best place to put initialization code.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //<editor-fold desc="Main application initializations">

        // run the original on Create method from the parent class AppCompatActivity
        // makes the default initializations that the application needs
        super.onCreate(savedInstanceState);
        // set the main view
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout);

        // Buttons init:
        selectPhotoButton = findViewById(R.id.selectPhotoButton);

        evalButton = findViewById(R.id.evalButton);
        showResultsButton = findViewById(R.id.showResultsButton);
        showResultsButton.setEnabled(false);
        // End of buttons init.

        // the view (panel) where the selected image is shown
        mainImageView = findViewById(R.id.mainImageView);
        // set the default image to be shown on application start
        this.currentPhotoBitmap = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.porsche_911_gts);

        // get saved cached data and restore the settings to the last available state
        sharedAppPref = getDefaultSharedPreferences(this);
        this.updateAppSettings();

        // TODO: discover how to change the dialog color

        // create a new activity result listener which gets image from the phone Gallery
        chooseFromGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (data != null) {
                            Uri selectedPhotoUri = data.getData();
                            mainImageView.setImageURI(selectedPhotoUri);
                            this.currentPhotoBitmap = ((BitmapDrawable) mainImageView
                                    .getDrawable()).getBitmap();
                        }
                    }
                });

        // create a new activity result listener which takes photo from the camera
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        this.currentPhotoBitmap = ImageBuilder.decodeAndShowPhoto(
                                this, currentPhotoPath, mainImageView
                        );
                    }
                });


        //</editor-fold>
    }

    /**
     * Creates an image selection dialog.
     * @param view The main view where the dialog will be created.
     */
    public void selectPhoto(View view) {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        int themeID = getDialogThemeID(true);
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this,
                themeID);
        DialogInterface.OnClickListener dialogOnClickL = (dialogInterface, i) -> {
            // different options user can choose
            switch (options[i]) {
                case takePhoto:
                    TakePhotoIntent();
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
        alertDiaBuilder.setItems(options, dialogOnClickL);
        alertDiaBuilder.show();
    }

    private int getDialogThemeID(boolean alternative) {
        int themeID = 0;
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            themeID = R.style.LightDialogTheme;
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            if (alternative) {
                themeID = R.style.AlternativeDialogTheme;
            } else {
                themeID = R.style.DarkDialogTheme;
            }
        }
        return themeID;
    }

    // Main entry point
    private void TakePhotoIntent() {
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
        // Intent describes the action that we want to do.
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File resultPhotoFile = null;
        try {
            // try to save the photo
            resultPhotoFile = ImageBuilder.createEmptyPhotoFile(this);
            currentPhotoPath = resultPhotoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if the file with the photo was created then continue
        if (resultPhotoFile != null) {
            Uri resultPhotoURI = FileProvider.getUriForFile( this,
                    "com.example.android.file_provider", resultPhotoFile);
            // save the actual photo to the photo file via an intent
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, resultPhotoURI);
            try {
                takePhotoLauncher.launch(takePictureIntent);
            } catch (Exception e) {
                System.out.println("Failed to start the camera intent.");
            }
        }
    }

    /**
     * Checks for permission. If no permission was granted, then asks for it.
     * @param permission Name of the permission (all permissions are located in Manifest.permission)
     * @param requestCode The request code of the permission. Permission codes are defined by
     *                    the programmer and are used for internal communication
     *                    between different methods.
     */
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }
    }


    public void startEvaluationProcess(View view) {
        String selectedMode = sharedAppPref.getString("mode", "");
        String[] modes = getResources().getStringArray(R.array.mode_values);

        if (selectedMode.equals(modes[0])) { // Direct evaluation
            this.createEvalImgConfirmDialog();
        }
        else if (selectedMode.equals(modes[1])) {
            this.createSendImgConfirmDialog();
        }
    }

    public void createEvalImgConfirmDialog() {
        int themeID = getDialogThemeID(false);
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this,
                themeID);
        String alertMessage = "The selected image will be evaluated. Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> handleModel())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    private void handleModel() {
        MLModel model = new MLModel(MainActivity.this);
        ModelResultPair[] results = model.evalDirectly(this.currentPhotoBitmap);
        evaluationResult = model.convertPairArrayToString(results);
        this.showAlertDialog(this.evaluationResult);
        this.showResultsButton.setEnabled(evaluationResult != null && !evaluationResult.equals(""));
    }

    public void createSendImgConfirmDialog() {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        String alertMessage = "The selected image will be sent to the server for evaluation. " +
                              "Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> handleServerCommunication())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    public void handleServerCommunication() {
        OkHTTP3Controller okHTTP3Controller = new OkHTTP3Controller(MainActivity.this,
                serverIP, portNumber);
        // If the controller IP address is private that means the server and the mobile device must
        // both be connected to it. Otherwise the data transfer won't work.
        // Port 5000 is a TCP/UDP port and is widely used.
        try {
            okHTTP3Controller.connectServer(this.currentPhotoBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Connection error..." + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Gets the result data and writes them on the selected view in a special dialog.
     * @param view The view where the dialog containing all the date will be shown.
     */
    public void getDirectResult(View view) {
        this.showAlertDialog(this.evaluationResult);
    }

    public void showAlertDialog(String message) {
        int themeID = getDialogThemeID(false);
        if (message != null && !message.equals("")) {
            new AlertDialog.Builder(MainActivity.this, themeID)
                    .setMessage(message) // write the result data on the dialog panel
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .setCancelable(false)
                    .show(); // show the dialog until the 'OK' button is clicked
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, MainSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAppSettings() {
        String theme = sharedAppPref.getString("theme", ""); // restore the theme
        ThemeController.setChosenTheme(theme);

        this.updateBackground();

        serverIP = sharedAppPref.getString("server_IP", "");
        portNumber = Integer.parseInt(sharedAppPref.getString("server_port", ""));
    }

    public static void setBackgroundColor(int color) {
        mainLayout.setBackgroundColor(color);
    }

    private void updateBackground() {
        if (ThemeController.chosenTheme == ThemeController.LIGHT) {
            setBackgroundColor(getColor(R.color.White));
        } else if (ThemeController.chosenTheme == ThemeController.DARK) {
            setBackgroundColor(getColor(R.color.CyberBlack));
        }
    }
}