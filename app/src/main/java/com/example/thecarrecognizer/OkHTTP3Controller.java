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
// Created by: Georgi S. Georgiev
// Main information source: https://square.github.io/okhttp/

/**
 * Class which defines the HTTP3 communication with the server part of the project.
 */
public class OkHTTP3Controller {
    private final MainActivity activity;
    private final String ipAddress;
    private final long port;
    private String responseText = "";

    /**
     * Main constructor of the class. Sets the destination IP and port.
     * @param mainActivity The main application activity.
     * @param ipAddress The IP address to which the application will send requests.
     * @param port The target port.
     */
    public OkHTTP3Controller(MainActivity mainActivity, String ipAddress, long port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.activity = mainActivity;
    }

    /**
     * Creates a POST request to the server and sends the image bitmap which has to be evaluated.
     * @param imgBitmap The image bitmap to be send to and evaluated by the server.
     */
    public void connectServer(Bitmap imgBitmap) {
        // Create a new URL address of the destination server.
        String postURL = "http://" + ipAddress + ":" + port + "/";
        if (ipAddress == null || ipAddress.equals("")) {
            String errorMes = "Trying to connect a null or an empty IP address.";
            // No IP set, show an error message to the user which will be shown directly
            // in the bottom of the main activity.
            activity.runOnUiThread(() -> Toast.makeText(activity, errorMes, Toast.LENGTH_LONG)
                    .show());
            return;
        }
        // Initialize a new output stream where the image bitmap will be put.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Save the image as a JPEG and don't change its quality. The result will be written
        // inside the stream.
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray(); // Convert the stream to a ByteArray.

        // Define the body of the HTML POST. Set the name of the image which will be sent
        // and its format. Then add the image to the POST body via the previous byteArray.
        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "car_img_0.jpg",
                        RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        activity.runOnUiThread(() -> Toast.makeText(activity, "Please wait ...",
                Toast.LENGTH_SHORT).show());

        // Send a request for a POST. Definition of the function right below.
        // Pass the destination URL and the body of the POST which contains the image.
        sendPostRequest(postURL, postBodyImage);
    }

    // Send a request for a POST.
    private void sendPostRequest(String postURL, RequestBody postMessageBody) {
        // Create a new OkHTTP client which represents the application end
        // of the communication channel.
        OkHttpClient client = new OkHttpClient();
        // Create a request with the given destination URL and the message body
        // (in our case the message body contains the .jpg image which has to be evaluated).
        Request request = new Request.Builder().url(postURL).post(postMessageBody).build();

        // Create a call via the above request and register a callback which is triggered on a
        // response from the server.
        client.newCall(request).enqueue(new Callback() {
            // If cancellation, a connectivity problem, or a timeout has occurred
            // then call the following function.
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                call.cancel(); // Connection failed, cancel the call.
                String errorMes = "Failed to Connect to Server. " + e.getMessage();
                // Show the response text in the main activity via a Toast message.
                activity.runOnUiThread(() -> Toast.makeText(activity, errorMes, Toast.LENGTH_LONG)
                                                  .show());
            }
            // On successful response call the following function:
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response resp) throws IOException {
                if (resp.body() == null) {
                    // No result was returned. There was some kind of an error.
                    throw new IOException("Empty return message body");
                }
                // The response body isn't empty, so convert it into a string.
                // In our case the server does all the necessary formatting of the response string
                // so we don't have to make any adjustments to it in here.
                responseText = resp.body().string();
                // Get the currently selected theme. It determines the theme mode of
                // the result dialog.
                int themeID = activity.getDialogThemeID();
                // Create the dialog with the result text written on it.
                // The Android environment requires the dialog to be displayed via a special UI
                // thread for increased application stability.
                activity.runOnUiThread(() -> new AlertDialog.Builder(activity, themeID)
                        .setMessage(responseText) // Write the result data on the dialog panel.
                        .setPositiveButton("OK", (dialog, id) -> enhancedDialogDismiss(dialog))
                        .setCancelable(false)
                        .show());
                // The "OK" button ensures that the result data will be saved internally in the
                // application main activity. So after clicking "OK" it is possible to see the
                // results via the "Show Results" button. The results then are shown in an
                // extended mode which allows the user to do a Google search.
            }
        });
    }

    // Saves the result in the corresponding variables of the main activity and activates the
    // "Show result" button. After that closes the dialog.
    private void enhancedDialogDismiss(DialogInterface dialog) {
        if (responseText != null && !responseText.equals("")) {
            this.activity.evaluationResult = responseText;
            // Split the first word of the response text from the rest.
            String[] firstWordExtractionArray = responseText.split(", ", 2);
            // If the result is Unknown then there won't be any internet browsing options.
            if (firstWordExtractionArray[0].equals("Unknown")) {
                this.activity.bestEvaluationResult = "";
            }
            else {
                // The first word represents the evaluation result class,
                // save it to the main activity.
                this.activity.bestEvaluationResult = firstWordExtractionArray[0];
                try {
                    // Load the class labels to the main activity. They are needed to determine
                    // which of them equals to the class of the best evaluation prediction we got.
                    this.activity.loadedLabels =
                            FileUtil.loadLabels(this.activity.getApplicationContext(),
                                    MLModel.ASSOCIATED_AXIS_LABELS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // If the evaluation was successful and the result is not null or empty, enable the
        // "Show Results" button.
        this.activity.showResultsButton
                .setEnabled(activity.evaluationResult != null &&
                        !activity.evaluationResult.equals(""));
        // Close the dialog which called this method.
        dialog.dismiss();
    }
}
