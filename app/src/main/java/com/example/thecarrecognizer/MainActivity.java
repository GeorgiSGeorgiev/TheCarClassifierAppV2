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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.*;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Created by: Georgi S. Georgiev

// Note 1: this whole project will be referred as "the application"
// Note 2: the main program which does the evaluation will be referred as "the core"

public class MainActivity extends AppCompatActivity {
    String evaluationResult;
    // main buttons
    Button selectPhotoButton;
    Button evalButton;
    Button showResultsButton;
    // the image which will be displayed on the application after selection
    ImageView mainImageView;
    public static int backgroundColor = Color.WHITE;

    // the main (and only) Google Drive Controller
    // manages user authentication and creates the connection to the Drive servers
    GoogleDriveController googleDriveController;

    // static variables representing the communication codes
    // created for better code readability
    static final int REQUEST_GOOGLE_SIGN_IN = 0;
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
        showResultsButton = findViewById(R.id.showResultsButton);
        showResultsButton.setEnabled(false);
        // End of buttons init.

        // the view (panel) where the selected image is shown
        mainImageView = findViewById(R.id.mainImageView);
        // the background
        final ImageView backgroundView = findViewById(R.id.backgroundImageView);
        // set the default image to be shown on application start
        this.currentPhotoBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.porsche_911_gts);

        // initialize the onClick event of the changeThemeButton
        // switches between dark and light mode (light mode is the default one)
        View.OnClickListener onThemeBtnClick = view -> {
            Button theB = (Button) view;
            switch (backgroundColor) {
                case Color.WHITE:
                    ViewExtensions.ChangeButtonColor(theB, Color.WHITE, Color.BLACK);
                    backgroundColor = Color.BLACK;
                    backgroundView.setBackgroundColor(Color.rgb(36,36,36));
                    // (36,36,36) is the cyber black color
                    break;
                case Color.BLACK:
                    ViewExtensions.ChangeButtonColor(theB, Color.BLACK, Color.WHITE);
                    backgroundColor = Color.WHITE;
                    backgroundView.setBackgroundColor(Color.WHITE);
                    break;
            }
        };
        changeThemeButton.setOnClickListener(onThemeBtnClick);
        //</editor-fold>
    }


    /**
     * Creates the Google Drive sign in dialog.
     * @param view The view where the dialog will be shown.
     */
    public void requestSignIn(View view) {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient googleClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(googleClient.getSignInIntent(), REQUEST_GOOGLE_SIGN_IN);
    }


    private static final String takePhoto = "Take Photo";
    private static final String chooseFromGallery = "Choose from Gallery";
    private static final String backStr = "Back";

    String currentPhotoPath = "";
    Bitmap currentPhotoBitmap;
    final String defaultAppDriveFolderName = "CarPhotoAndInfo_TheCarClassifierApp";

    // Used for the pseudo-unique folder name generation.
    Random rand = new Random();
    // Warning! Contains statically limited generator. May cause a problem if the application gets bigger and well-known.
    String pseudoUniqueName = String.valueOf(System.currentTimeMillis()) + rand.nextInt(100000000);


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
                    Intent intent1 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
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
     * @param requestCode The request code of the permission. Permission codes are defined by the programmer and
     *                    are used for internal communication between different methods.
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
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GOOGLE_SIGN_IN:
                    handleSignInIntent(data);
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    this.currentPhotoBitmap = ImageBuilder.decodeAndShowPhoto(this, currentPhotoPath, mainImageView);
                    this.evalButton.setEnabled(true);
                    this.showResultsButton.setEnabled(false);
                    break;
                case REQUEST_CHOOSE_FROM_GALLERY:
                    Uri selectedPhotoUri = data.getData();
                    mainImageView.setImageURI(selectedPhotoUri);
                    this.currentPhotoBitmap = ((BitmapDrawable)mainImageView.getDrawable()).getBitmap();
                    this.evalButton.setEnabled(true);
                    this.showResultsButton.setEnabled(false);
                    break;
            }
        }
    }

    // Creates a new Drive controller and handles the Sign in event.
    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(googleSignInAccount -> {
                    // on success we already have the user credentials and we have to do the sign in operation
                    GoogleAccountCredential credential = GoogleAccountCredential
                            .usingOAuth2(MainActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleSignInAccount.getAccount());

                    Drive googleDriveService = new Drive
                            .Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                            .setApplicationName("The Car Classifier")
                            .build();

                    googleDriveController = new GoogleDriveController(googleDriveService,  pseudoUniqueName + '-' + this.defaultAppDriveFolderName);

                    createUploadPhotoWarningDialog();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                });
    }

    private void createUploadPhotoWarningDialog() {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        String alertMessage = "The selected image will be uploaded to your Google Drive. Do you wish to continue?";
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Yes", (dialog, id) -> uploadImage())
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    public void createEvalPhotoConfirmDialog(View view) {
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
        System.out.println(evaluationResult);
        if (evaluationResult != null) {
            this.showResultsButton.setEnabled(true);
        }
    }

    /**
     * Uploads the selected by the user image to Google Drive. If there are move files with the same ID on the cloud
     * deletes them at the beginning.
     */
    public void uploadImage() {
        // delete existing files with the same Drive ID
        googleDriveController.safeDelete();

        // converts the selected image to Java file
        File resultFile = ImageBuilder.convertBitmapToFile(this, this.currentPhotoBitmap);

        // progress dialog
        AlertDialog dialog = ProgressDialogBuilder.CreateAlertDialog(this, R.layout.progress_bar_dialog_layout);
        dialog.show();

        googleDriveController.createImageFile(this, resultFile, pseudoUniqueName, "Result.txt","CarPhoto.png")
                .addOnSuccessListener(s -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Upload was successful", Toast.LENGTH_LONG).show();
                    this.evalButton.setEnabled(false);
                    this.showResultsButton.setEnabled(true);
                    // on each successful upload change the pseudoUniqueName
                    pseudoUniqueName = String.valueOf(System.currentTimeMillis()) + rand.nextInt(100000000);
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Check your Google Drive API key", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Gets the result data from Drive and writes them on the selected view in a special dialog.
     * @param view The view where the dialog containing all the date will be shown.
     */
    public void getResult(View view) {
        boolean resultAvailable = googleDriveController.checkForResult();
        System.out.println(resultAvailable);
        // if the result is still not available, the user has to try to get the data later
        if (!resultAvailable) {
            Toast.makeText(getApplicationContext(), "Result file is not ready. Please wait a little bit and then try again.", Toast.LENGTH_LONG).show();
            return;
        }
        // get from Drive, save on the current device and then show the result data
        googleDriveController.getAndSave(this);
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        googleDriveController.safeDeleteWithNull();
        this.evalButton.setEnabled(true);
        this.showResultsButton.setEnabled(false);
        // GoogleDriveController.folderDriveID = null;
    }

    /**
     * Gets the result data from Drive and writes them on the selected view in a special dialog.
     * @param view The view where the dialog containing all the date will be shown.
     */
    public void getDirectResult(View view) {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDiaBuilder.setMessage(evaluationResult) // write the result data on the dialog panel
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss()); // define 'OK' button
        // show the dialog until the 'OK' button is clicked
        alertDiaBuilder.show();
        this.evalButton.setEnabled(true);
    }
}