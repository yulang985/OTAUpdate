package cn.wch.blelib.host.core.callback;

import android.bluetooth.BluetoothGatt;

public interface PhyReadCallback {
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status);
}
