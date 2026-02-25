package com.example.snapsenseai;

import android.util.Log;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiClient {
    private static final String API_KEY = "AIzaSyB2-M4B_VbHKVylGBx3y_ZNlcoetoaIr4U"; // Replace with your key
    // The specific ID for the stable Flash-Lite model
    // 1. Update the Model ID to the Gemini 3 Flash version
    // 1. Revert to the Gemini 2.5 Flash identifier
    private static final String MODEL = "gemini-2.5-flash";

    // 2. The URL must target the v1beta endpoint for this model
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    public interface GeminiCallback {
        void onSuccess(String category, int ttlHours, String suggestedName);
        void onFailure(Exception e);
    }

    public void classifyContent(String redactedText, GeminiCallback callback) {
        new Thread(() -> {
            try {
                // Increase timeout to 30 seconds for slow campus/mobile networks
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                // Refined prompt for rigid JSON output
                String prompt = "Analyze this redacted screenshot text: '" + redactedText + "'. " +
                        "Classify it into one category (Finance, Study, Social, Travel, or OTP). " +
                        "Determine if it is 'Temporary' or 'Permanent'. If temporary, provide TTL in hours. " +
                        "Suggest a short file name. Return ONLY valid JSON: " +
                        "{\"category\": \"string\", \"ttl\": int, \"name\": \"string\"}";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("contents", new org.json.JSONArray().put(new JSONObject()
                        .put("parts", new org.json.JSONArray().put(new JSONObject().put("text", prompt)))));

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder().url(URL).post(body).build();

                Log.d("SnapSense", "Sending request to Gemini API...");

                try (Response response = client.newCall(request).execute()) {
                    // LOG 1: The HTTP status code (200 is success, 403 is key error, 429 is limit)
                    Log.d("SnapSense", "HTTP Response Code: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        String result = response.body().string();

                        // LOG 2: The actual raw response from Google
                        Log.d("SnapSense", "Raw Gemini Response: " + result);

                        parseGeminiResponse(result, callback);
                    } else {
                        // LOG 3: Specific error message if the request failed
                        String errorMsg = response.body() != null ? response.body().string() : "No error body";
                        Log.e("SnapSense", "Server Error: " + response.message() + " | Body: " + errorMsg);
                        callback.onFailure(new Exception("HTTP " + response.code() + ": " + response.message()));
                    }
                }
            } catch (Exception e) {
                // LOG 4: Catches networking errors like timeouts or DNS issues
                Log.e("SnapSense", "Network/JSON Exception: " + e.getMessage());
                callback.onFailure(e);
            }
        }).start();
    }
    private void parseGeminiResponse(String responseBody, GeminiCallback callback) {
        try {
            // Simplified parsing for demonstration; in production, use GSON
            JSONObject json = new JSONObject(responseBody);
            String textResponse = json.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text");

            JSONObject result = new JSONObject(textResponse.replace("```json", "").replace("```", ""));
            callback.onSuccess(result.getString("category"), result.getInt("ttl"), result.getString("name"));
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}