package cn.wch.blelib.peripheral.builder;

import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

import androidx.annotation.NonNull;


public class BluetoothGattDescriptorBuilder {
    private final UUID uuid;
    private final int permissions;

    public BluetoothGattDescriptorBuilder(@NonNull UUID uuid, int permissions) {
        this.uuid = uuid;
        this.permissions=permissions;
    }

    public BluetoothGattDescriptor build(){
        BluetoothGattDescriptor descriptor=new BluetoothGattDescriptor(uuid,permissions);

        return descriptor;
    }
}
