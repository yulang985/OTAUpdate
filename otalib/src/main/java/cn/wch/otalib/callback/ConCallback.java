package cn.wch.otalib.callback;

import android.bluetooth.BluetoothDevice;

public interface ConCallback {
    void OnError(String mac, Throwable t);

    void OnConnecting(String mac);

    void OnConnectSuccess(String mac);

    //characteristic not match
    void OnInvalidDevice(String mac);

    void OnConnectTimeout(String mac);

    void OnDisconnect(String mac, BluetoothDevice bluetoothDevice, int status);
}
