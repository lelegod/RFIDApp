package jp.co.sharedmodule;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.ConnectionEventHandler;


    public class BluetoothConnectionService extends Service {

        private static final String TAG = "BluetoothConnectionService";
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
        public IBinder onBind(Intent intent) {
            return binder;
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
    }

