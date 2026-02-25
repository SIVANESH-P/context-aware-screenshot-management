package com.example.snapsenseai;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DeleteWorker extends Worker {

    public DeleteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve the data we passed in (URI and Name)
        String uriString = getInputData().getString("image_uri");
        String fileName = getInputData().getString("file_name");

        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            Log.d("SnapSense", "TTL Expired for: " + fileName);

            // Trigger the Notification instead of direct deletion
            NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
            notificationHelper.showDeletePrompt(imageUri, fileName);

            return Result.success();
        }

        return Result.failure();
    }
}