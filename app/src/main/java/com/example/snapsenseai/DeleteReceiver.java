package com.example.snapsenseai;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.Collections;

public class DeleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String uriString = intent.getStringExtra("uri");
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);

            // This triggers the official Android system popup to confirm deletion
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                PendingIntent pi = MediaStore.createTrashRequest(context.getContentResolver(),
                        Collections.singletonList(uri), true);
                try {
                    // Note: This usually requires an Activity context to launch the dialog
                    context.startIntentSender(pi.getIntentSender(), null, 0, 0, 0);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }
}