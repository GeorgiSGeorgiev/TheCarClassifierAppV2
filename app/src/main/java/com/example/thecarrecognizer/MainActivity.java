package com.example.thecarrecognizer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;


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



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button changeThemeButton = findViewById(R.id.changeThemeButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);

        remoteEvalButton = findViewById(R.id.remoteEvalButton);

        mainImageView = findViewById(R.id.mainImageView);

        final ImageView backgroundView = findViewById(R.id.backgroundImageView);

        requestSignIn();

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

        View.OnClickListener onRemoteEvalClick = this::uploadImage;

        changeThemeButton.setOnClickListener(onThemeBtnClick);
        remoteEvalButton.setOnClickListener(onRemoteEvalClick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void requestSignIn() {
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



    public void selectPhoto(View view) {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        //alertDiaBuilder.setTitle("");
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
            resultPhotoFile = createPhotoFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if the file with the photo was created then continue
        if (resultPhotoFile != null) {
            //System.out.println(resultPhotoFile.toPath());
            Uri resultPhotoURI = FileProvider.getUriForFile( this,
                    "com.example.android.file_provider", resultPhotoFile);
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
        else {
            Toast.makeText(MainActivity.this,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
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
                    decodeAndShowPhoto();
                    break;
                case REQUEST_CHOOSE_FROM_GALLERY:
                    Uri selectedPhotoUri = data.getData();
                    currentPhotoPath = selectedPhotoUri.getPath();
                    mainImageView.setImageURI(selectedPhotoUri);
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

                    googleDriveController = new GoogleDriveController(googleDriveService);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                });
    }

    public void uploadImage(View view) {
        AlertDialog dialog = ProgressDialogBuilder.CreateAlertDialog(this);
        dialog.show();

        File tempFile = new File(this.getCacheDir(), "temp.png");

        Bitmap bitmap = decodePhoto();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapData = bos.toByteArray();

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(tempFile);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        googleDriveController.CreateImageFile(tempFile)
                .addOnSuccessListener(s -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Upload was successful", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Check your Google Drive API key", Toast.LENGTH_LONG).show();
                });
    }

    private File createPhotoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photo = File.createTempFile(
                photoFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = photo.getAbsolutePath();
        System.out.println(currentPhotoPath);
        return photo;
    }

    private Bitmap decodePhoto() {
        Bitmap resultBitmap;
        if (!currentPhotoPath.equals("")) {
            int targetWidth = mainImageView.getWidth();
            int targetHeight = mainImageView.getHeight();

            BitmapFactory.Options bmfOptions = new BitmapFactory.Options();
            bmfOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(currentPhotoPath, bmfOptions);

            int photoWidth = bmfOptions.outWidth;
            int photoHeight = bmfOptions.outHeight;

            int scaleFactor = Math.max(1, Math.min(photoWidth / targetWidth, photoHeight / targetHeight));

            bmfOptions.inJustDecodeBounds = false;
            bmfOptions.inSampleSize = scaleFactor;
            resultBitmap = BitmapFactory.decodeFile(currentPhotoPath, bmfOptions);

        } else {
            resultBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.porsche_911_gts);
        }
        return resultBitmap;
    }

    private void decodeAndShowPhoto() {
        Bitmap resultBitmap = decodePhoto();
        mainImageView.setImageBitmap(resultBitmap);
    }
}