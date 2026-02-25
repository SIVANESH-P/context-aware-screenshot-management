package com.example.snapsenseai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "SnapSense_Lifecycle";
    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screenshot Lifecycle", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for expired screenshots");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public void showDeletePrompt(Uri uri, String fileName) {
        Intent deleteIntent = new Intent(context, DeleteReceiver.class);
        deleteIntent.putExtra("uri", uri.toString());

        // Use unique request code to avoid intent collision
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                (int) System.currentTimeMillis(), deleteIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_delete)
                .setContentTitle("SnapSense: File Expired")
                .setContentText("The screenshot '" + fileName + "' is ready for deletion.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Ensure you have POST_NOTIFICATIONS permission for Android 13+
        NotificationManagerCompat.from(context).notify((int)System.currentTimeMillis(), builder.build());
    }
}