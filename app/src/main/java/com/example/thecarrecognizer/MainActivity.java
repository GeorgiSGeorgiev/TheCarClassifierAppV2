package com.example.thecarrecognizer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.*;

// Created by: Georgi S. Georgiev

// Note 1: this whole project will be referred as "the application"
// Note 2: the main program which does the evaluation will be referred as "the core"

public class MainActivity extends AppCompatActivity {
    String evaluationResult;
    // Main buttons
    Button selectPhotoButton;
    Button evalButton;
    Button showResultsButton;
    Button changeModButton;
    // The image which will be displayed on the application after selection
    ImageView mainImageView;
    public static int backgroundColor = Color.WHITE;
    // Mod controls
    int selectedMod = 1;
    String[] mods = { "Send To Server", "Direct Evaluation"};

    private static final String takePhoto = "Take Photo";
    private static final String chooseFromGallery = "Choose from Gallery";
    private static final String backStr = "Back";

    String currentPhotoPath = "";
    Bitmap currentPhotoBitmap;

    // static variables representing the communication codes
    // created for better code readability
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CHOOSE_FROM_GALLERY = 2;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;


    // This method is called automatically on application start.
    // It is the best place to put initialization code.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //<editor-fold desc="Main application initializations">

        // run the original on Create method from the parent class AppCompatActivity
        // makes the default initializations that the application needs
        super.onCreate(savedInstanceState);
        // set the main view (panel)
        setContentView(R.layout.activity_main);

        // Buttons init:
        Button changeThemeButton = findViewById(R.id.changeThemeButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);

        evalButton = findViewById(R.id.evalButton);
        evalButton.setOnClickListener(this::createEvalImgConfirmDialog);
        showResultsButton = findViewById(R.id.showResultsButton);
        showResultsButton.setEnabled(false);
        changeModButton = findViewById(R.id.changeModeButton);
        changeModButton.setText(mods[this.selectedMod]);
        // End of buttons init.

        // the view (panel) where the selected image is shown
        mainImageView = findViewById(R.id.mainImageView);
        // the background
        final ImageView backgroundView = findViewById(R.id.backgroundImageView);
        // set the default image to be shown on application start
        this.currentPhotoBitmap = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.porsche_911_gts);

        // initialize the onClick event of the changeThemeButton
        // switches between dark and light mode (light mode is the default one)
        View.OnClickListener onThemeBtnClick = view -> {
            Button themeButton = (Button) view;
            switch (backgroundColor) {
                case Color.WHITE:
                    ViewExtensions.ChangeButtonColor(themeButton, Color.WHITE,
                            getColor(R.color.CyberBlack));
                    ViewExtensions.ChangeButtonColor(changeModButton, getColor(R.color.CyberBlack),
                            Color.WHITE);
                    backgroundColor = Color.BLACK;
                    backgroundView.setBackgroundColor(getColor(R.color.CyberBlack));
                    break;
                case Color.BLACK:
                    ViewExtensions.ChangeButtonColor(themeButton, Color.BLACK, Color.WHITE);
                    ViewExtensions.ChangeButtonColor(changeModButton, Color.WHITE,
                            getColor(R.color.CyberBlack));
                    backgroundColor = Color.WHITE;
                    backgroundView.setBackgroundColor(Color.WHITE);
                    break;
            }
        };
        changeThemeButton.setOnClickListener(onThemeBtnClick);
        //</editor-fold>
    }

    /**
     * Changes the selected mod. Updates the button text as well.
     * @param view The main application view.
     */
    public void changeMode(View view) {
        if (this.selectedMod == 0) {
            this.selectedMod = 1;
            this.evalButton.setOnClickListener(this::createEvalImgConfirmDialog);
        } else if (this.selectedMod == 1) {
            this.selectedMod = 0;
            this.evalButton.setOnClickListener(this::createSendImgConfirmDialog);
        }
        this.changeModButton.setText(mods[this.selectedMod]);
    }


    /**
     * Creates an image selection dialog.
     * @param view The main view where the dialog will be created.
     */
    public void selectPhoto(View view) {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        DialogInterface.OnClickListener dialogOnClickL = (dialogInterface, i) -> {
            // different options user can choose
            switch (options[i]) {
                case takePhoto:
                    TakePhotoIntent();
                    break;
                case chooseFromGallery:
                    Intent intent1 = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    startActivityForResult(intent1, REQUEST_CHOOSE_FROM_GALLERY);
                    break;
                case backStr:
                    dialogInterface.dismiss();
                    break;
            }
        };
        alertDiaBuilder.setItems(options, dialogOnClickL);
        alertDiaBuilder.show();
    }

    // Main entry point
    private void TakePhotoIntent() {
        // Intent describes the action that we want to do.
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
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
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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

    /**
     * Method that handles different user requests.
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through setResult().
     * @param data An Intent, which can return result data to the caller
     *             (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    this.currentPhotoBitmap = ImageBuilder.decodeAndShowPhoto(
                            this, currentPhotoPath, mainImageView
                    );
                    this.showResultsButton.setEnabled(false);
                    break;
                case REQUEST_CHOOSE_FROM_GALLERY:
                    Uri selectedPhotoUri = data.getData();
                    mainImageView.setImageURI(selectedPhotoUri);
                    this.currentPhotoBitmap = ((BitmapDrawable)mainImageView
                            .getDrawable()).getBitmap();
                    this.showResultsButton.setEnabled(false);
                    break;
            }
        }
    }

    public void createEvalImgConfirmDialog(View view) {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        String alertMessage = "The selected image will evaluated. Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> handleModel())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    private void handleModel() {
        MLModel model = new MLModel(MainActivity.this);
        ModelResultPair[] results = model.evalDirectly(this.currentPhotoBitmap);
        evaluationResult = model.convertPairArrayToString(results);
        // System.out.println(evaluationResult);
        this.showAlertDialog(this.evaluationResult);
        this.showResultsButton.setEnabled(evaluationResult != null && !evaluationResult.equals(""));
    }

    public void createSendImgConfirmDialog(View view) {
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
                "192.168.11.144", 5000);
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
        if (message != null && !message.equals("")) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message) // write the result data on the dialog panel
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .setCancelable(false)
                    .show(); // show the dialog until the 'OK' button is clicked
        }
    }
}