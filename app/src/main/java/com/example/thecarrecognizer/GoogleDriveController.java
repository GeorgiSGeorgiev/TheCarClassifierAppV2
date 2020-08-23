package com.example.thecarrecognizer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveController {

    // An object that executes submitted Runnable tasks
    private final Executor driveExecutor = Executors.newSingleThreadExecutor();
    private Drive driveService;

    public GoogleDriveController(Drive driveService) {
        this.driveService = driveService;
    }

    public Task<String> CreateImageFile(java.io.File inputFile) {
        return Tasks.call(driveExecutor, () -> {
            File fileMetaData = new File();
            fileMetaData.setName("TheImage");

            FileContent mediaContent = new FileContent("application/png", inputFile);

            File driveFile = null;
            try {
                driveFile = driveService.files().create(fileMetaData, mediaContent).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (driveFile == null) {
                throw new IOException("The result drive file is null after creation call.");
            }
            return driveFile.getId();
        });
    }
}
