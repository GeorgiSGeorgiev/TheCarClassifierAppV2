package com.example.thecarrecognizer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            String folderName = "CarPhotos_TheCarClassifier";
            File driveFolder;
            // Create new folder
            File folderMetadata = new File();

            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            driveFolder = driveService.files().create(folderMetadata)
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
            FileContent mediaContent = new FileContent("image/png", inputFile);

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
            /*List<File> fl = retrieveAllFiles(this.driveService);
            for (int i = 0; i < fl.size(); i++) {
                System.out.println(fl.get(i).getName());
            }
            System.out.flush();
            System.out.println();*/
            //WriteAllFiles();
            return driveFile.getId();
        });
    }

    private static List<File> retrieveAllFiles(Drive service) throws IOException {
        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getFiles());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }

    public void WriteAllFiles() {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        /*.setSpaces("drive")*/
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                System.out.printf("Found file: %s (%s)\n",
                        file.getName(), file.getId());
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
    }
}
