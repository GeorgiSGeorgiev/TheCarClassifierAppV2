package com.example.thecarrecognizer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHTTP3Controller {
    private final MainActivity activity;
    private final String ipAddress;
    private final long port;
    private String responseText = "";

    public OkHTTP3Controller(MainActivity mainActivity, String ipAddress, long port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.activity = mainActivity;
    }

    public void connectServer(Bitmap imgBitmap) {
        String postURL = "http://" + ipAddress + ":" + port + "/";
        if (ipAddress == null || ipAddress.equals("")) {
            String errorMes = "Trying to connect a null or an empty IP address.";
            activity.runOnUiThread(() -> Toast.makeText(activity, errorMes, Toast.LENGTH_LONG)
                    .show());
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "car_img_0.jpg",
                        RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        activity.runOnUiThread(() -> Toast.makeText(activity, "Please wait ...",
                Toast.LENGTH_SHORT).show());

        sendPostRequest(postURL, postBodyImage);
    }

    private void sendPostRequest(String postURL, RequestBody postMessageBody) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(postURL).post(postMessageBody).build();

        client.newCall(request).enqueue(new Callback() {
            // cancellation, a connectivity problem, or a timeout has occurred
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                call.cancel(); // connection failed, cancel the call
                String errorMes = "Failed to Connect to Server. " + e.getMessage();
                activity.runOnUiThread(() -> Toast.makeText(activity, errorMes, Toast.LENGTH_LONG)
                                                  .show());
            }
            // successful response
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                if (resp.body() == null) {
                    throw new IOException("Empty return message body");
                }
                responseText = resp.body().string();
                int themeID = activity.getDialogThemeID();
                // System.out.println(responseText);
                activity.runOnUiThread(() -> new AlertDialog.Builder(activity, themeID)
                        .setMessage(responseText) // write the result data on the dialog panel
                        .setPositiveButton("OK", (dialog, id) -> enhancedDialogDismiss(dialog))
                        .setCancelable(false)
                        .show());
            }
        });
    }

    // Saves the result in the corresponding variables of the main activity and activates the
    // "Show result" button. After that closes the dialog.
    private void enhancedDialogDismiss(DialogInterface dialog) {
        if (responseText != null && !responseText.equals("")) {
            this.activity.evaluationResult = responseText;
            String[] firstWordExtractionArray = responseText.split(", ", 2);
            // If the result is Unknown then there won't be any internet browsing options.
            if (firstWordExtractionArray[0].equals("Unknown")) {
                this.activity.bestEvaluationResult = "";
            }
            else {
                this.activity.bestEvaluationResult = firstWordExtractionArray[0];
                try {
                    this.activity.loadedLabels =
                            FileUtil.loadLabels(this.activity.getApplicationContext(),
                                    MLModel.ASSOCIATED_AXIS_LABELS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.activity.showResultsButton
                .setEnabled(activity.evaluationResult != null &&
                        !activity.evaluationResult.equals(""));
        dialog.dismiss();
    }
}
