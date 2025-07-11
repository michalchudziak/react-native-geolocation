package com.reactnativecommunity.geolocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.facebook.react.bridge.Callback;

public class LocationService extends Service implements LocationHandler {
    private static final String TAG = "LocationService";
    private final IBinder binder = new LocalBinder();
    private static final String CHANNEL_ID = "com.reactnativecommunity.geolocation.location_service";
    private static final int NOTIFICATION_ID = 1;
    private LocationHandler mLocationHandler;

    public class LocalBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        stopLocationUpdates();  // Clean up resources
    }

    // will be called when `startService(Intent)` being called
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setLocationHandler(LocationHandler locationHandler) {
        this.mLocationHandler = locationHandler;
    }

    public void startLocationUpdates(LocationOptions options) {
        Log.i(TAG, "startLocationUpdates");
        mLocationHandler.startLocationUpdates(options);
    }

    public void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates");
        mLocationHandler.stopLocationUpdates();
    }

    public void getCurrentLocation(LocationOptions options, final Callback success,
                                   Callback error) {
        Log.i(TAG, "getCurrentLocation");
        this.mLocationHandler.getCurrentLocation(options, success, error);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. create a channel
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "GDL Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            // 2. register the notification channel
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}