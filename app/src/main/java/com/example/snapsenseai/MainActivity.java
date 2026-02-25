package com.example.snapsenseai;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SnapSense";

    // --- Phase 4 Retry Logic Variables ---
    private FileOrganizer fileOrganizer;
    private Uri pendingUri;
    private String pendingCategory;
    private String pendingName;

    // The "Launcher" that waits for you to click "Allow" on the system popup
    private final ActivityResultLauncher<IntentSenderRequest> intentSenderLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "User granted permission. Retrying move...");
                    if (pendingUri != null) {
                        fileOrganizer.moveAndRename(pendingUri, pendingCategory, pendingName);
                        Toast.makeText(this, "Screenshot Categorized!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "User denied permission to move file.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Phase 4 tools
        fileOrganizer = new FileOrganizer(this);

        // Register Phase 1 Screenshot Observer
        ScreenshotObserver observer = new ScreenshotObserver(new Handler(), this);
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
        );

        Log.d(TAG, "SnapSense Core: Active and Monitoring.");
    }

    // This method will be called by FileOrganizer when it hits a SecurityException
    public void requestFilePermission(IntentSenderRequest request, Uri uri, String cat, String name) {
        this.pendingUri = uri;
        this.pendingCategory = cat;
        this.pendingName = name;
        intentSenderLauncher.launch(request);
    }
}