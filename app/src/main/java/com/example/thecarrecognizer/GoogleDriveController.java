package com.example.thecarrecognizer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.*;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class that connects the application with the Google Drive services.
 */
public class GoogleDriveController {

    // An object that executes submitted Runnable tasks
    private final Executor driveExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService;
    public final String DefaultApplicationFolderName;

    // stores the unique ID of the desired Drive folder
    public static String folderDriveID = null;

    private String imageFileDriveID = null;
    private String resultTextFileID = null;
    private String resultFilePath = null;

    /**
     * Main constructor.
     * @param driveService The main Drive Service this class will be working with.
     * @param defaultApplicationFolderName The default Drive folder name where files uploaded to the cloud will be stored.
     */
    public GoogleDriveController(Drive driveService, String defaultApplicationFolderName) {
        this.driveService = driveService;
        this.DefaultApplicationFolderName = defaultApplicationFolderName;
    }

    /**
     * Returns a task which puts a local image file to an image file located in a new folder (with the default name) on Drive.
     * The name of the text file where the result will be stored and the image to be processed are created of two parts.
     * First of them has to be unique (pseudo-unique). It is an unique ID that can be used by the core program.
     * (The core program is the main program that is getting the images from the other side of the Drive communication and processes them.)
     * @param context The main application context.
     * @param inputFile The source image file to be reformatted and uploaded to Drive.
     * @param pseudoUniqueName The first part of the new result file and the image file names which has to be unique (or pseudo-unique).
     * @param fileName The second part of the result file name which can be arbitrary ("the user-friendly name").
     * @param imageName The second part of the image name which can be arbitrary ("the user-friendly name").
     * @return A task which creates the requested folder on Drive and puts the image and the result file in it.
     */
    public Task<String> createImageFile(Context context, java.io.File inputFile, String pseudoUniqueName, String fileName, String imageName) {
        return Tasks.call(driveExecutor, () -> {

            // Create a new folder with the given name
            File driveFolder = this.createNewDriveFolderFromTheRoot(DefaultApplicationFolderName);
            folderDriveID = driveFolder.getId();
            this.addNewWritePermissionToUser(folderDriveID, "gogi.gig.99@gmail.com");

            // Create new file that contains the car photo and put it inside the new Drive folder
            File imageFile = this.createNewImagePNGFile(pseudoUniqueName + '-' + imageName, driveFolder.getId(), inputFile);
            this.imageFileDriveID = imageFile.getId();
            this.addNewWritePermissionToUser(this.imageFileDriveID, "gogi.gig.99@gmail.com");

            // Create the file where the result of the evaluation will be stored.
            File resultTextFile = this.createEmptyTextFile(context,pseudoUniqueName + '-' + fileName, driveFolder.getId());
            this.resultTextFileID = resultTextFile.getId();
            this.addNewWritePermissionToUser(resultTextFileID, "gogi.gig.99@gmail.com");

            return imageFile.getId();
        });
    }


    /**
     * Creates a new Drive folder in the root drive directory.
     * @param folderName The name of the folders we are creating.
     * @throws IOException Error with the Drive folder creation.
     */
    public File createNewDriveFolderFromTheRoot(String folderName) throws IOException {
        File driveFolder;
        // Create new folder
        File folderMetadata = new File();

        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        driveFolder = this.driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        if (driveFolder == null) {
            throw new IOException("The result drive folder is null after its creation.");
        }
        return driveFolder;
    }

    /**
     * Creates a new Drive file and puts the input image in it.
     * @param name The name of the Drive file with the image in it.
     * @param parentID The ID of the parent folder where the Drive file will be located.
     * @param imageContent The local image file to be reformatted and uploaded to Drive.
     * @return The result Drive File with the image in it.
     * @throws IOException Error with the Drive file creation.
     */
    public File createNewImagePNGFile(String name, String parentID, java.io.File imageContent) throws IOException {
        File driveFile;

        File fileMetaData = new File();
        fileMetaData.setName(name);
        fileMetaData.setParents(Collections.singletonList(parentID));

        // the content of the Image file
        FileContent mediaContent = new FileContent("image/png", imageContent);
        driveFile = driveService.files().create(fileMetaData, mediaContent).execute();

        if (driveFile == null) {
            throw new IOException("The result drive file is null after its creation.");
        }
        return  driveFile;
    }

    /**
     * Creates an empty text Drive file. (the evaluation result will be stored inside)
     * @param context The main application context. Used to create an empty local text file in the Downloads directory.
     * @param name The name of the text Drive file.
     * @param parentID The ID of the parent folder where the Drive file will be located.
     * @return The result Drive text File which is empty.
     * @throws IOException Error with the Drive file creation.
     */
    public File createEmptyTextFile(Context context, String name, String parentID) throws IOException {
        File driveFile;

        File fileMetaData = new File();
        fileMetaData.setName(name);
        fileMetaData.setParents(Collections.singletonList(parentID));
        java.io.File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        java.io.File emptyFile = null;
        // try to create an empty text file in the Environment.DIRECTORY_DOWNLOADS directory
        try {
            emptyFile = java.io.File.createTempFile(
                    "tempResultFile",
                    ".txt",
                    directory
            );
            this.resultFilePath = emptyFile.getAbsolutePath();
        } catch (IOException e) {
        e.printStackTrace();
        }
        if (emptyFile == null) { // file creation failed => throw IO exception
            throw new IOException();
        }

        // create new Drive file containing the empty text file
        FileContent mediaContent = new FileContent("text/plain", emptyFile);
        driveFile = driveService.files().create(fileMetaData, mediaContent).execute();

        if (driveFile == null) {
            throw new IOException("The result drive file is null after its creation.");
        }
        return  driveFile;
    }

    /**
     * Calls in a new thread (required to have safe code prune to deadlocks) a method which downloads the result file to
     * the local text file and then gets the result data from that file (this method is called after the evaluation of
     * the data is finished and the result is present) and writes all the data on a special result dialog.
     * @param context The main application context. Used to create the result dialog.
     */
    public void getAndSave(Context context) {
        this.driveExecutor.execute(() -> getAndSave_Unthreadsafe(context));
    }

    // the unsafe method which is called by the method above
    private void getAndSave_Unthreadsafe(Context context) {
        try {
            // download from Drive the text file containing all the results
            // the core program must have been done the evaluation by this moment
            this.downloadResultTextFile(context);

            // load the downloaded data to the application logic
            java.io.File file = new java.io.File(this.resultFilePath);
            FileInputStream fin = new FileInputStream(file);
            InputStream inputStream = new BufferedInputStream(fin);

            // Below is an output stream needed for the on-screen output handling.
            // The data from the result text file will be saved in that stream.
            // This eases the data manipulation in the application logic.
            // ByteArrayOutputStream can easily read all kind of data and convert them to a string.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            int i;
            try {
                // read first character from the already downloaded local result file
                i = inputStream.read();
                while (i != -1) {
                    // write the recently read character to the output stream
                    byteArrayOutputStream.write(i);
                    i = inputStream.read(); // continue reading characters until EOF
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace(); // if there is an reading error we do not want from the application to crash
            }
            // create a dialog which writes all the data saved in the output stream
            this.createResultDialog(context, byteArrayOutputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // gets the result data in the form of a string and writes them on an AlertDialog which appears to the user
    private void createResultDialog(Context context, String resultData) {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(context);
        alertDiaBuilder.setMessage(resultData) // write the result data on the dialog panel
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss()); // define 'OK' button
        Looper.prepare();
        // show the dialog until the 'OK' button is clicked
        // looper is needed to show on-screen text from a thread different from the main one
        alertDiaBuilder.show();
        Looper.loop();
    }

    /**
     * Grants the user write permission on the specified Drive file/folder.
     * @param fileID Drive ID of the targeted file/folder.
     * @param userEmailAddress The email address of the user.
     * @throws IOException Creating a new Drive permission may fail and throw an IO exception.
     */
    public void addNewWritePermissionToUser(String fileID, String userEmailAddress) throws IOException {
        // set new user permissions
        Permission userPermission = new Permission()
                .setType("user")
                .setRole("writer")
                .setEmailAddress(userEmailAddress);

        // applied on a folder the following command adds the new permission to all other files and folders inside
        driveService.permissions().create(fileID, userPermission)
                .setFields("id").execute();
    }

    /**
     * Downloads from Drive the file containing the result data.
     * @param context Main application context.
     * @throws IOException The file download may throw an IO exception.
     */
    public void downloadResultTextFile(Context context) throws IOException {
        File file = driveService.files().get(this.resultTextFileID).setFields("size").execute();
        // if the result file on Drive is empty then there was some kind of connection error to the core program
        if (file.getSize().intValue() == 0) {
            Looper.prepare();
            Toast.makeText(context, "Connection error occurred please try later", Toast.LENGTH_LONG).show();
            Looper.loop();
            return;
        }
        java.io.File theFile = new java.io.File(this.resultFilePath);
        OutputStream outputStream = new FileOutputStream(theFile);
        // command that directly downloads the requested file to the local file (via the output stream created above)
        driveService.files().get(this.resultTextFileID).executeMediaAndDownloadTo(outputStream);
    }

    static boolean exists = false;

    /**
     * Checks if the result file contains the evaluation data.
     * @return  True/False according to the state of the result file.
     */
    public boolean checkForResult() {
        exists = false;
        Executor executor = Executors.newSingleThreadExecutor(); // apply the check on a different thread (prevent dead-locks)
        executor.execute(
                ()-> {
                    File file = null;
                    try {
                        file = driveService.files().get(this.resultTextFileID).setFields("size").execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    exists = (file != null && file.getSize().intValue() != 0);
                    System.out.println(exists);
                });
        try {
            TimeUnit.SECONDS.sleep(1); // gives the other thread a big chance to complete its evaluation
            // this waiting needs to be improved in the future
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return exists;
    }

    /**
     * Deletes the whole folder which contains the Image and the result data from the Drive cloud.
     * Calls the deletion on a different thread. Dead-lock proof.
     */
    public void safeDelete() {
        // Possible issue: on application crash the data won't be deleted from Drive and after restart the application
        // can not delete them because it has lost their ID.
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(
                ()-> {
                    try {
                        if (folderDriveID != null) {
                            driveService.files().delete(folderDriveID).execute();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    /**
     * Deletes the whole folder which contains the Image and the result data from the Drive cloud and sets folderDriveID to null.
     */
    public void safeDeleteWithNull() {
        // has the same issue as above
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(
                ()-> {
                    try {
                        if (folderDriveID != null) {
                            driveService.files().delete(folderDriveID).execute();
                            folderDriveID = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    /**
     * Deletes the whole folder with the given ID from the Drive cloud. Can not cause dead-lock because is ran on a different thread.
     * @param fileID The ID of the file to be deleted safely.
     */
    public void safeDelete(String fileID) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(
                () -> {
                    try {
                        if (fileID != null) {
                            driveService.files().delete(fileID).execute();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    /**
     * Write all Drive files present in the root directory of the logged user.
     * Calls the main writing method on a different thread.
     */
    public void writeAllFiles() {
        this.driveExecutor.execute(this::writeAllFiles_Unthreadsafe);
    }

    // main writing method
    private void writeAllFiles_Unthreadsafe() {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                // get all files from all the present Drive pages ("Page tokens") (pages containing all the files)
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                // print the files which were found
                for (File file : result.getFiles()) {
                    System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
            // go to the next page
            if (result != null) {
                pageToken = result.getNextPageToken();
            }        } while (pageToken != null);
        System.out.flush();
    }
}
