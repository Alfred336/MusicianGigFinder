package com.musicianfinder.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.musicianfinder.R;
import com.musicianfinder.activities.MusicianDashboardActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "gig_finder_channel";
    private static final String CHANNEL_NAME = "Gig Finder Notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = "Musician Gig Finder";
        String body  = "";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().getOrDefault("title", title);
            body  = remoteMessage.getData().getOrDefault("body",  body);
        }

        sendNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        // TODO: Send new token to your server / Firestore musician doc
        // firestoreDb.collection("musicians").document(uid).update("fcmToken", token)
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MusicianDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
