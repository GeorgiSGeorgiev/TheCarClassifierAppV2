package com.example.thecarrecognizer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveController {

    // An object that executes submitted Runnable tasks
    private final Executor driveExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService;

    public GoogleDriveController(Drive driveService) {
        this.driveService = driveService;
    }

    public Task<String> CreateImageFile(java.io.File inputFile, String fileName) {
        return Tasks.call(driveExecutor, () -> {
            // Create new folder
            File folderMetadata = new File();
            folderMetadata.setName("CarPhotos_TheCarClassifier");
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            File driveFolder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            if (driveFolder == null) {
                throw new IOException("The result drive folder is null after its creation.");
            }


            // Create new file that contains the car photo and put it inside the new Drive folder
            File fileMetaData = new File();
            fileMetaData.setName(fileName);
            fileMetaData.setParents(Collections.singletonList(driveFolder.getId()));

            // the content of the Image file
            FileContent mediaContent = new FileContent("application/png", inputFile);

            File driveFile = null;
            try {
                // add the Image to Drive
                driveFile = driveService.files().create(fileMetaData, mediaContent).execute();
                // set new user permissions
                Permission userPermission = new Permission()
                        .setType("user")
                        .setRole("writer")
                        .setEmailAddress("gogi.gig.99@gmail.com"/*"cars.bmw.99@gmail.com"*/);
                /*driveService.permissions().create(driveFile.getId(), userPermission)
                        .setFields("id").execute();*/
                // add the new permissions to the whole newly created Drive folder
                driveService.permissions().create(driveFolder.getId(), userPermission)
                        .setFields("id").execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (driveFile == null) {
                throw new IOException("The result drive file is null after its creation.");
            }

            return driveFile.getId();
        });
    }
}
