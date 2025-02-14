package cn.wch.blelib.host.core.callback;

import android.bluetooth.BluetoothGatt;

public interface MTUCallback {
    void onMTUChanged(BluetoothGatt gatt, int mtu, int status);
}
