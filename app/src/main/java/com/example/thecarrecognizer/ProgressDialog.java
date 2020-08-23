package com.example.thecarrecognizer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

class ProgressDialogBuilder {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static AlertDialog CreateAlertDialog(Context currentContext) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentContext);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(R.layout.progress_bar_dialog_layout);
        AlertDialog resultDialog = builder.create();
        return resultDialog;
    }
}
