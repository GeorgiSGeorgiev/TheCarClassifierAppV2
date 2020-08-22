package com.example.thecarrecognizer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.loader.ResourcesLoader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Button selectPhotoButton;
    Button remoteEvalButton;
    ImageView mainImageView;
    public static int backgroundColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button changeThemeButton = findViewById(R.id.changeThemeButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);

        remoteEvalButton = findViewById(R.id.remoteEvalButton);

        mainImageView = findViewById(R.id.mainImageView);

        final ImageView backgroundView = findViewById(R.id.backgroundImageView);

        View.OnClickListener onThemeBtnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        };

        View.OnClickListener onPhotoSelect = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto();
            }
        };

        View.OnClickListener onRemoteEvalClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //File newFile = new File("https://drive.google.com/drive/folders/18_F9BIliBzKUSTk5JUmTqn1Jjp2F7IV0?usp=sharing", "tmpCarPhoto.png");
                Bitmap decodedRecentPhoto = decodePhoto();
                String urlText = "https://drive.google.com/drive/folders/18_F9BIliBzKUSTk5JUmTqn1Jjp2F7IV0?usp=sharing";

            }
        };

        changeThemeButton.setOnClickListener(onThemeBtnClick);
        selectPhotoButton.setOnClickListener(onPhotoSelect);
        remoteEvalButton.setOnClickListener(onRemoteEvalClick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private static final String takePhoto = "Take Photo";
    private static final String chooseFromGallery = "Choose from Gallery";
    private static final String backStr = "Back";

    String currentPhotoPath = "";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CHOOSE_FROM_GALLERY = 2;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private void selectPhoto() {
        final String[] options = { takePhoto, chooseFromGallery, backStr };
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(MainActivity.this);
        //alertDiaBuilder.setTitle("");
        DialogInterface.OnClickListener dialogOnClickL = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                decodeAndShowPhoto();
            }
            else if (requestCode == REQUEST_CHOOSE_FROM_GALLERY) {
                Uri selectedPhotoUri = data.getData();
                currentPhotoPath = selectedPhotoUri.getPath();
                mainImageView.setImageURI(selectedPhotoUri);
            }
        }
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
        Bitmap resultBitmap = null;
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