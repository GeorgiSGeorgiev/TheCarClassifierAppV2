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
import android.view.Menu;
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


public class MainActivity extends AppCompatActivity {
    Button selectPhotoButton;
    Button remoteEvalButton;
    ImageView mainImageView;
    public static int backgroundColor = Color.WHITE;

    GoogleDriveController googleDriveController;


    static final int REQUEST_GOOGLE_SIGN_IN = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CHOOSE_FROM_GALLERY = 2;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;


    // This method is called on application start
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button changeThemeButton = findViewById(R.id.changeThemeButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);

        remoteEvalButton = findViewById(R.id.remoteEvalButton);

        mainImageView = findViewById(R.id.mainImageView);

        final ImageView backgroundView = findViewById(R.id.backgroundImageView);

        this.currentPhotoBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.porsche_911_gts);

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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


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



    public void selectPhoto(View view) {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        DialogInterface.OnClickListener dialogOnClickL = (dialogInterface, i) -> {
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

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }
    }

    // Activity handling method
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
                    break;
                case REQUEST_CHOOSE_FROM_GALLERY:
                    Uri selectedPhotoUri = data.getData();
                    mainImageView.setImageURI(selectedPhotoUri);
                    this.currentPhotoBitmap = ((BitmapDrawable)mainImageView.getDrawable()).getBitmap();
                    break;
            }
        }
    }

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

                    googleDriveController = new GoogleDriveController(googleDriveService, this.defaultAppDriveFolderName);
                    createUploadPhotoWarningDialog();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                });
    }

    private void createUploadPhotoWarningDialog() {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        String alertMessage = String.format("Warning! To keep your Google Drive root directory clean the application will delete all files and folders named: %s", this.defaultAppDriveFolderName);
        alertDiaBuilder.setMessage(alertMessage)
                .setPositiveButton("Proceed", (dialog, id) -> uploadImage())
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
        alertDiaBuilder.show();
    }

    public void uploadImage() {
        File resultFile = ImageBuilder.convertBitmapToFile(this, this.currentPhotoBitmap);

        AlertDialog dialog = ProgressDialogBuilder.CreateAlertDialog(this, R.layout.progress_bar_dialog_layout);
        dialog.show();
        googleDriveController.createImageFile(resultFile, "TheCarClassifier_TheCarPhoto01.png")
                .addOnSuccessListener(s -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Upload was successful", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Check your Google Drive API key", Toast.LENGTH_LONG).show();
                });
    }
}