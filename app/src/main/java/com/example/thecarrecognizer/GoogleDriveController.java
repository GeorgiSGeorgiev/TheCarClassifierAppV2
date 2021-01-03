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

public class GoogleDriveController {

    // An object that executes submitted Runnable tasks
    private final Executor driveExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService;
    public final String DefaultApplicationFolderName;

    public static String folderDriveID = null;

    private String imageFileDriveID = null;
    private String resultTextFileID = null;
    private String resultFilePath = null;

    public GoogleDriveController(Drive driveService, String defaultApplicationFolderName) {
        this.driveService = driveService;
        this.DefaultApplicationFolderName = defaultApplicationFolderName;
    }

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

            File resultTextFile = this.createEmptyTextFile(context,pseudoUniqueName + '-' + fileName, driveFolder.getId());
            this.resultTextFileID = resultTextFile.getId();
            this.addNewWritePermissionToUser(resultTextFileID, "gogi.gig.99@gmail.com");

            //writeAllFiles();
            return imageFile.getId();
        });
    }


    /**
     * Creates a new Drive folder in the root drive directory.
     * @param folderName The name of the folders we are creating.
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

    public File createEmptyTextFile(Context context, String name, String parentID) throws IOException {
        File driveFile;

        File fileMetaData = new File();
        fileMetaData.setName(name);
        fileMetaData.setParents(Collections.singletonList(parentID));
        java.io.File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        java.io.File emptyFile = null;
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
        if (emptyFile == null) {
            throw new IOException();
        }

        FileContent mediaContent = new FileContent("text/plain", emptyFile);
        driveFile = driveService.files().create(fileMetaData, mediaContent).execute();

        if (driveFile == null) {
            throw new IOException("The result drive file is null after its creation.");
        }
        return  driveFile;
    }

    public void getAndSave(Context context) {
        this.driveExecutor.execute(() -> getAndSave_Unthreadsafe(context));
    }

    private void getAndSave_Unthreadsafe(Context context) {
        try {
            //System.out.println(this.resultTextFileID);
            this.downloadResultTextFile(context);

            java.io.File file = new java.io.File(this.resultFilePath);
            FileInputStream fin = new FileInputStream(file);
            InputStream inputStream = new BufferedInputStream(fin);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            int i;
            try {
                i = inputStream.read();
                while (i != -1) {
                    byteArrayOutputStream.write(i);
                    i = inputStream.read();
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.createResultDialog(context, byteArrayOutputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createResultDialog(Context context, String resultData) {
        AlertDialog.Builder alertDiaBuilder = new AlertDialog.Builder(context);
        alertDiaBuilder.setMessage(resultData)
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        Looper.prepare();
        alertDiaBuilder.show();
        Looper.loop();
    }

    public void addNewWritePermissionToUser(String fileID, String userEmailAddress) throws IOException {
        // set new user permissions
        Permission userPermission = new Permission()
                .setType("user")
                .setRole("writer")
                .setEmailAddress(userEmailAddress);

        // add the new permissions to the whole newly created Drive folder
        driveService.permissions().create(fileID, userPermission)
                .setFields("id").execute();
    }

    public void downloadResultTextFile(Context context) throws IOException {
        File file = driveService.files().get(this.resultTextFileID).setFields("size").execute();
        // System.out.println(file.getSize());
        if (file.getSize().intValue() == 0) {
            Looper.prepare();
            Toast.makeText(context, "Connection error occurred please try later", Toast.LENGTH_LONG).show();
            Looper.loop();
            return;
        }
        java.io.File theFile = new java.io.File(this.resultFilePath);
        OutputStream outputStream = new FileOutputStream(theFile);
        driveService.files().get(this.resultTextFileID).executeMediaAndDownloadTo(outputStream);
    }

    static boolean exists = false;
    public boolean checkForResult() {
        exists = false;
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(
                ()-> {
                    File file = null;
                    try {
                        file = driveService.files().get(this.resultTextFileID).setFields("size").execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // System.out.println(file.getSize());
                    exists = (file != null && file.getSize().intValue() != 0);
                    System.out.println(exists);
                });
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return exists;
    }

    public void safeDelete() {
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

    public void safeDeleteWithNull() {
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

    public void writeAllFiles() {
        this.driveExecutor.execute(this::writeAllFiles_Unthreadsafe);
    }

    private void writeAllFiles_Unthreadsafe() {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            if (result != null) {
                pageToken = result.getNextPageToken();
            }        } while (pageToken != null);
        System.out.flush();
    }
}
