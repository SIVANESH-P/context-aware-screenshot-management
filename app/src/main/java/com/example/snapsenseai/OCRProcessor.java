package com.example.snapsenseai;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRProcessor {
    private final Context context;
    private final TextRecognizer recognizer;
    private final PIIRedactor redactor;
    private final GeminiApiClient geminiClient;
    private final FileOrganizer fileOrganizer;

    public OCRProcessor(Context context) {
        this.context = context;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.redactor = new PIIRedactor();
        this.geminiClient = new GeminiApiClient();
        this.fileOrganizer = new FileOrganizer(context);
    }

    public void processScreenshot(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        Log.d("SnapSense", "Raw OCR Extracted: " + rawText);

                        // 1. Local Privacy Scrubbing
                        String safeText = redactor.redact(rawText);
                        Log.d("SnapSense", "Privacy-Preserved Text: " + safeText);

                        // 2. Update Global Buffer
                        AnonymizedMetadataBuffer.getInstance().setBuffer(safeText);

                        // 3. Trigger Phase 3: Semantic Cloud Classification
                        Log.d("SnapSense", "Requesting AI Classification from Gemini Flash...");
                        geminiClient.classifyContent(safeText, new GeminiApiClient.GeminiCallback() {
                            @Override
                            public void onSuccess(String category, int ttlHours, String suggestedName) {
                                // 1. Move and Rename first (Phase 4)
                                fileOrganizer.moveAndRename(imageUri, category, suggestedName);

                                // 2. Prepare data for the Worker
                                androidx.work.Data inputData = new androidx.work.Data.Builder()
                                        .putString("image_uri", imageUri.toString())
                                        .putString("file_name", suggestedName)
                                        .build();

                                // 3. Create the WorkRequest with the AI's TTL
                                androidx.work.OneTimeWorkRequest deleteWorkRequest =
                                        new androidx.work.OneTimeWorkRequest.Builder(DeleteWorker.class)
                                                .setInitialDelay(ttlHours, java.util.concurrent.TimeUnit.HOURS)
                                                .setInputData(inputData)
                                                .addTag("SCREENSHOT_LIFECYCLE")
                                                .build();

                                // 4. Schedule it!
                                androidx.work.WorkManager.getInstance(context).enqueue(deleteWorkRequest);

                                Log.d("SnapSense", "Lifecycle Scheduled: Deletion prompt in " + ttlHours + " hours.");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("SnapSense", "Gemini Classification Failed: " + e.getMessage());
                            }
                        });
                    })
                    .addOnFailureListener(e -> Log.e("SnapSense", "OCR Failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.e("SnapSense", "Error loading image: " + e.getMessage());
        }
    }
}