package com.example.thecarrecognizer;

import android.net.Uri;
import android.view.View;

public interface PhotoUtilityActivityInterface {
    void selectPhoto(View view);
    void handleTakePhotoIntent();
    void decodeAndShowPhotoFromUri(Uri photoURI);
    void showAlertDialog(String message, Boolean cancelable);
}
