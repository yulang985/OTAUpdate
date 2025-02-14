package cn.wch.blelib.host.core.callback;

import android.bluetooth.BluetoothGatt;

public interface PhyUpdateCallback {
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status);
}
