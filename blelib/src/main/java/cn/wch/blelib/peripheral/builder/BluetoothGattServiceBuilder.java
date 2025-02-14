package cn.wch.blelib.peripheral.builder;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class BluetoothGattServiceBuilder {
    private final UUID uuid;
    private final int serviceType;
    private ArrayList<BluetoothGattCharacteristic> characteristicList;

    public BluetoothGattServiceBuilder(UUID uuid, int serviceType) {
        this.uuid = uuid;
        this.serviceType = serviceType;
        characteristicList=new ArrayList<>();
    }

    public void addCharacteristics(Collection<BluetoothGattCharacteristic> characteristics){
        characteristicList.addAll(characteristics);
    }

    public void addCharacteristic(BluetoothGattCharacteristic characteristic){
        characteristicList.add(characteristic);
    }

    public BluetoothGattService build(){
        BluetoothGattService service = new BluetoothGattService(uuid, serviceType);
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            service.addCharacteristic(characteristic);
        }
        return service;
    }
}
