package com.example.snapsenseai;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

public class ScreenshotObserver extends ContentObserver {
    private final Context context;
    private final OCRProcessor ocrProcessor;

    // Fix: Tracks the last time a screenshot was processed to ignore duplicate system signals
    private long lastTriggerTime = 0;
    private static final long DEBOUNCE_DELAY = 3000; // 3 seconds window

    // Fix: Tracks the last processed URI to prevent re-processing the same file metadata update
    private String lastProcessedUri = "";

    public ScreenshotObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
        this.ocrProcessor = new OCRProcessor(context);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        long currentTime = System.currentTimeMillis();

        // 1. Debounce Check: Ignore if triggered within 3 seconds of the last one
        if (currentTime - lastTriggerTime < DEBOUNCE_DELAY) {
            return;
        }

        // Handle null URIs
        if (uri == null) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        // 2. Exact URI Check: Prevent re-triggering for the same file's metadata update
        if (uri.toString().equals(lastProcessedUri)) {
            return;
        }

        // Check if it's a media update
        if (uri.toString().contains(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) {
            lastTriggerTime = currentTime;
            lastProcessedUri = uri.toString();
            handleNewScreenshot(uri);
        }
    }

    private void handleNewScreenshot(Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.Images.Media.DISPLAY_NAME},
                null, null, "_id DESC")) { // Order by ID to get the latest entry

            if (cursor != null && cursor.moveToFirst()) {
                String fileName = cursor.getString(0);

                // Only process if it's actually a screenshot
                if (fileName != null && fileName.toLowerCase().contains("screenshot")) {
                    Log.d("SnapSense", "Verified New Screenshot: " + fileName);

                    // TRIGGER PHASE 2: Start On-Device OCR
                    ocrProcessor.processScreenshot(uri);
                }
            }
        } catch (Exception e) {
            Log.e("SnapSense", "Error verifying screenshot: " + e.getMessage());
        }
    }
}