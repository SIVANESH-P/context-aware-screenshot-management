package com.example.snapsenseai;

import android.app.RecoverableSecurityException;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.IntentSenderRequest;

import java.util.Arrays;
import java.util.List;

public class FileOrganizer {
    private Context context;
    private final List<String> validCategories = Arrays.asList("Finance", "Study", "Social", "Travel", "OTP");

    public FileOrganizer(Context context) {
        this.context = context;
    }

    public void moveAndRename(Uri sourceUri, String category, String newName) {
        try {
            // Check if the category is recognized, else move to Miscellaneous
            String finalCategory = validCategories.contains(category) ? category : "Miscellaneous";

            ContentValues values = new ContentValues();

            // Rename the file with a timestamp to ensure uniqueness
            values.put(MediaStore.Images.Media.DISPLAY_NAME, newName + "_" + (System.currentTimeMillis() / 1000));

            // Set the new path under Pictures/SnapSenseAI/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SnapSenseAI/" + finalCategory);
            }

            // Attempt to update the MediaStore (This triggers the move/rename)
            int rowsUpdated = context.getContentResolver().update(sourceUri, values, null, null);

            if (rowsUpdated > 0) {
                Log.d("SnapSense", "Successfully moved to: " + finalCategory + " as " + newName);
            }

        } catch (SecurityException securityException) {
            // Handle Scoped Storage permission issue on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    securityException instanceof RecoverableSecurityException) {

                RecoverableSecurityException recoverableException = (RecoverableSecurityException) securityException;

                Log.d("SnapSense", "Scoped Storage restriction hit. Requesting user permission...");

                // Create the IntentSenderRequest for the new Activity Result API
                IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                        recoverableException.getUserAction().getActionIntent().getIntentSender()).build();

                // Call the helper method in MainActivity to launch the system dialog
                if (context instanceof MainActivity) {
                    ((MainActivity) context).requestFilePermission(
                            intentSenderRequest,
                            sourceUri,
                            category,
                            newName
                    );
                }
            } else {
                Log.e("SnapSense", "Persistent Security Error: " + securityException.getMessage());
            }
        } catch (Exception e) {
            Log.e("SnapSense", "General Error in FileOrganizer: " + e.getMessage());
        }
    }
}