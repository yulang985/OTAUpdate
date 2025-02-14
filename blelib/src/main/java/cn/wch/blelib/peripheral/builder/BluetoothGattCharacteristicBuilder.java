package cn.wch.blelib.peripheral.builder;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import androidx.annotation.NonNull;

public class BluetoothGattCharacteristicBuilder {
    private final UUID uuid;
    private final int properties;
    private final int permissions;
    private ArrayList<BluetoothGattDescriptor> descriptorList;

    public BluetoothGattCharacteristicBuilder(@NonNull UUID uuid, int properties, int permissions) {
        this.uuid = uuid;
        this.properties = properties;
        this.permissions = permissions;
        descriptorList=new ArrayList<>();
    }

    public void addDescriptors(Collection<BluetoothGattDescriptor> descriptors){
        descriptorList.addAll(descriptors);
    }

    public void addDescriptor(BluetoothGattDescriptor descriptor){
        descriptorList.add(descriptor);
    }

    public BluetoothGattCharacteristic build(){
        BluetoothGattCharacteristic characteristic=new BluetoothGattCharacteristic(uuid, properties, permissions);
        for (BluetoothGattDescriptor descriptor : descriptorList) {
            characteristic.addDescriptor(descriptor);
        }
        return characteristic;
    }
}
