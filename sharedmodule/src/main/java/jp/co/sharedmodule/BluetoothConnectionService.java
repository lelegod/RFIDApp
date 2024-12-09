package jp.co.sharedmodule;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.ConnectionEventHandler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.ConnectionEventHandler;

public class BluetoothConnectionService extends Service {

    private static final String TAG = "BluetoothConnectionService";
    private static final String CHANNEL_ID = "BluetoothServiceChannel";
    private static BluetoothConnectionService instance;

    private final IBinder binder = new LocalBinder();
    private BluetoothDevice connectedDevice;
    private TecRfidSuite mLib = TecRfidSuite.getInstance();
    private boolean isConnected = false;

    public class LocalBinder extends Binder {
        public BluetoothConnectionService getService() {
            return BluetoothConnectionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Create a notification channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Start as a foreground service
        startForeground(1, createNotification("Bluetooth Service Running"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service will stay alive until explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        // Safely disconnect when service is destroyed
        closeConnection();
        stopForeground(true);
        super.onDestroy();
    }

    // Static instance for easy access
    public static BluetoothConnectionService getInstance() {
        return instance;
    }

    // Method to initialize and connect to the device
    public void connectDevice(String deviceAddress) {
        if (mLib.getState() == TecRfidSuite.OPOS_S_CLOSED) {
            int result = mLib.open("UF-2200", this);
            if (result == TecRfidSuite.OPOS_SUCCESS) {
                mLib.claimDevice(deviceAddress, new ConnectionEventHandler() {
                    @Override
                    public void onEvent(int state) {
                        isConnected = (state == TecRfidSuite.ConnectStateOnline);
                        if (!isConnected) {
                            closeConnection();
                        }
                    }
                });
                mLib.setDeviceEnabled(true);
                isConnected = true;
            } else {
                // Log or handle error in connection initialization
            }
        }
    }

    // Method to disconnect the device
    public void closeConnection() {
        if (isConnected) {
            mLib.setDeviceEnabled(false);
            mLib.releaseDevice();
            mLib.close();
            isConnected = false;
        }
    }

    // Method to check if the device is connected
    public boolean isConnected() {
        return isConnected;
    }

    // Create a notification channel (for Android O and above)
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    // Create a foreground service notification
    private Notification createNotification(String contentText) {
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Bluetooth Connection Service")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        return builder.build();
    }
}
