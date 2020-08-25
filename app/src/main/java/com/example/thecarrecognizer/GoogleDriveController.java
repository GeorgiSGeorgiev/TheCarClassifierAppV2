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
    public final String DefaultApplicationFolderName;

    public GoogleDriveController(Drive driveService, String defaultApplicationFolderName) {
        this.driveService = driveService;
        this.DefaultApplicationFolderName = defaultApplicationFolderName;
    }

    public Task<String> createImageFile(java.io.File inputFile, String fileName) {
        return Tasks.call(driveExecutor, () -> {
            String folderName = DefaultApplicationFolderName;

            // Delete folders with the same name
            this.deleteAllFoldersAndFilesWithTheName(folderName);

            // Create a new folder with the given name
            File driveFolder = this.createNewDriveFolderFromTheRoot(folderName);

            // Create new file that contains the car photo and put it inside the new Drive folder
            File imageFile = this.createNewImagePNGFile(fileName, driveFolder.getId(), inputFile);

            this.addNewWritePermissionToUser(driveFolder.getId(), "gogi.gig.99@gmail.com");

            //writeAllFiles();
            return imageFile.getId();
        });
    }


    /**
     * Checks if there are any files or folders with the given name. If such files or folders exist then this method deletes them.
     * It is recommended to warn the user that these files or folders will be deleted and to give him the weapon of choice.
     * This method is necessary to keep the main Drive folder clean from unnecessary files and folders created by this application.
     * @param fileName The name of the files or folders we are looking for.
     */
    public void deleteAllFoldersAndFilesWithTheName(String fileName) {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        /*.setSpaces("drive")*/
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .setQ(String.format("name='%s'", fileName)) // query that searches files with specific name
                        .execute();
                for (File file : result.getFiles()) {
                    this.driveService.files().delete(file.getId()).execute();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            if (result != null) {
                pageToken = result.getNextPageToken();
            }
        } while (pageToken != null);
    }

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

    public void writeAllFiles() {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        /*.setSpaces("drive")*/
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
