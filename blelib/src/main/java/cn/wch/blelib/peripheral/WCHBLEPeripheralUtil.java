package cn.wch.blelib.peripheral;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import cn.wch.blelib.exception.BLEPeripheralException;
import cn.wch.blelib.peripheral.callback.BLEServerCallback;
import cn.wch.blelib.utils.FormatUtil;
import cn.wch.blelib.utils.LogUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class WCHBLEPeripheralUtil {
    private static WCHBLEPeripheralUtil wchblePeripheralUtil;

    private BluetoothManager mBluetoothManager;
    //广播
    private BluetoothLeAdvertiser mBluetoothAdvertiser;
    private AdvertiseCallback advertiseCallback;
    //GattServer
    private BluetoothGattServer mGattServer;

    //config
    private BLEServerCallback bleServerCallback;
    //notify
    private final int notifyTimeout=10000;
    private enum NOTIFY_STATE{
        STATE_PREPARE,
        STATE_SUCCESS,
        STATE_FAIL;
    }

    private NOTIFY_STATE notify_state=NOTIFY_STATE.STATE_PREPARE;
    private boolean stopNotifyFlag=false;
    //MTU
    private int currentMTU=23;
    private final int mtuOffset=3;

    //device
    private boolean isConnected=false;


    public static WCHBLEPeripheralUtil getInstance() {
        if(wchblePeripheralUtil==null){
            wchblePeripheralUtil=new WCHBLEPeripheralUtil();
        }
        return wchblePeripheralUtil;
    }

    public WCHBLEPeripheralUtil() {

    }

    //
    public void init(Application application) throws BLEPeripheralException {
        if(BluetoothAdapter.getDefaultAdapter()==null) {
            throw new BLEPeripheralException("BluetoothAdapter is null");
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            throw new BLEPeripheralException("该功能仅支持 Android 5.0及以上系统");
        }else {
            if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
                throw new BLEPeripheralException("该设备不支持广播功能");
            }
        }
        if (mBluetoothAdvertiser == null) {
            mBluetoothAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        }
        mBluetoothManager = (BluetoothManager) (application.getSystemService(Context.BLUETOOTH_SERVICE));
    }

    /**
     *开始广播
     * @param settings 广播设置
     * @param data 广播数据
     * @param scanResponse 扫描响应数据
     * @param callback 广播回调
     * 开启广播可能失败,errorCode为1,指蓝牙名称太长,需修改
     */
    public void startBroadcast(@NonNull AdvertiseSettings settings, @NonNull AdvertiseData data, @Nullable AdvertiseData scanResponse, @NonNull AdvertiseCallback callback) throws BLEPeripheralException {
        if(mBluetoothAdvertiser!=null){
            this.advertiseCallback=callback;
            if(scanResponse==null){
                mBluetoothAdvertiser.startAdvertising(settings, data, advertiseCallback);
            }else {
                mBluetoothAdvertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);
            }
        }else {
            throw new BLEPeripheralException("BluetoothLeAdvertiser is null");
        }

    }

    /**
     * 停止广播
     * @throws BLEPeripheralException
     */
    public void stopBroadcast() {
        if(mBluetoothAdvertiser!=null){
            if(advertiseCallback!=null){
                mBluetoothAdvertiser.stopAdvertising(advertiseCallback);
                advertiseCallback=null;
            }else {
                LogUtil.d("AdvertiseCallback is null");
            }
        }else {
            LogUtil.d("BluetoothLeAdvertiser is null");
        }
    }

    public void closeBroadcast(){
        if(mBluetoothAdvertiser!=null){
            if(advertiseCallback!=null){
                mBluetoothAdvertiser.stopAdvertising(advertiseCallback);
                advertiseCallback=null;
            }
        }
    }

    public void openGattServer(@NonNull Context context, @Nullable ArrayList<BluetoothGattService> services) throws BLEPeripheralException {
        if(mGattServer!=null){
            throw new BLEPeripheralException("BluetoothGattServer is running,invoke closeGattServer() first");
        }
        if (mBluetoothManager != null) {
            mGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
            if (mGattServer == null) {
                throw new BLEPeripheralException("BluetoothGattServer is null");
            }
            if(mGattServer!=null){
                for (BluetoothGattService service : services) {
                    mGattServer.addService(service);
                }
            }
        }
    }

    public void setServerCallback(BLEServerCallback bleServerCallback){
        this.bleServerCallback=bleServerCallback;
    }

    public BluetoothGattServer getCurrentGattServer(){
        return mGattServer;
    }

    public void disconnect(@NonNull BluetoothDevice device){
        if(device==null){
            return;
        }
        if(mGattServer!=null){
            mGattServer.cancelConnection(device);
        }
    }

    public void closeGattServer(){
        if(mGattServer!=null){
            mGattServer.close();
            mGattServer=null;
        }
        isConnected=false;

    }

    public boolean sendResponse(BluetoothDevice device, int requestId,
                             int status, int offset, byte[] value){
        if(mGattServer==null){
            return false;
        }
        LogUtil.d("sendResponse");
        return mGattServer.sendResponse(device, requestId, status, offset, value);
    }

    public boolean connect(BluetoothDevice device, boolean autoConnect){
        if(mGattServer==null){
            return false;
        }
        return mGattServer.connect(device, autoConnect);
    }

    public void stopNotify(){
        stopNotifyFlag=true;
    }

    public boolean isNotifyEnabled(UUID service,UUID characteristic)throws BLEPeripheralException{
        LogUtil.d("isNotifyEnabled");
        if(getCurrentGattServer()==null){
            throw new BLEPeripheralException("gattServer is null");
        }
        BluetoothGattService targetService = getCurrentGattServer().getService(service);
        if(targetService==null){
            throw new BLEPeripheralException("service don't exist");
        }
        BluetoothGattCharacteristic targetCharacteristic = targetService.getCharacteristic(characteristic);
        if(targetCharacteristic==null){
            throw new BLEPeripheralException("characteristic don't exist");
        }

        BluetoothGattDescriptor descriptor = targetCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if(descriptor==null){
            throw new BLEPeripheralException("this characteristic don't contain 00002902-0000-1000-8000-00805f9b34fb descriptor");
        }
        LogUtil.d("descriptor :"+FormatUtil.bytesToHexString(descriptor.getValue()));
        return Arrays.equals(descriptor.getValue(),BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    public int notifyCharacteristicChanged(BluetoothDevice device,
                                               BluetoothGattCharacteristic characteristic, boolean confirm,
                                               @NonNull byte[] buffer,int length){
        int total=0;
        int realLength=Math.min(buffer.length,length);
        //
        stopNotifyFlag=false;
        //分包
        int maxPacketLen = getMTU()-mtuOffset;
        int packageCount=realLength/maxPacketLen;
        for (int i = 0; i < packageCount; i++) {
            byte[] tmp=new byte[maxPacketLen];
            System.arraycopy(buffer,i*maxPacketLen,tmp,0,maxPacketLen);
            boolean b = notifyCharacteristicChanged(device, characteristic, confirm, tmp);
            if(!b){
                return total;
            }
            total+=tmp.length;
            if(i==(packageCount-1) && realLength%maxPacketLen==0){
                break;
            }
        }
        byte[] tmp=new byte[realLength%maxPacketLen];
        if(tmp.length!=0) {
            System.arraycopy(buffer, packageCount * maxPacketLen, tmp, 0, tmp.length);
            if (!notifyCharacteristicChanged(device, characteristic, confirm, tmp)) {
                return total;
            }
            LogUtil.d("final write "+tmp.length);
            total+=tmp.length;
        }
        return total;
    }



    public boolean notifyCharacteristicChanged(BluetoothDevice device,
                                               BluetoothGattCharacteristic characteristic, boolean confirm,
                                               @NonNull byte[] newValue){
        if(stopNotifyFlag){
            return false;
        }
        if(mGattServer==null|| device==null || characteristic==null){
            return false;
        }
        if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0){
            return false;
        }
        LogUtil.d(String.format(Locale.getDefault(),"notifyCharacteristicChanged(%d)：%s", newValue.length, FormatUtil.bytesToHexString(newValue)));
        notify_state=NOTIFY_STATE.STATE_PREPARE;
        if(!(characteristic.setValue(newValue) && mGattServer.notifyCharacteristicChanged(device, characteristic, confirm))){
            return false;
        }

        return notifyWaitTimeout(notifyTimeout);
    }

    public boolean notifyWaitTimeout(int timeout){

        int t=timeout;
        while (t>0){
            if(notify_state==NOTIFY_STATE.STATE_SUCCESS){
                return true;
            }else if(notify_state==NOTIFY_STATE.STATE_FAIL){
                return false;
            }
            t--;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LogUtil.d("notify wait Timeout!");
        return false;
    }


    private final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            LogUtil.d("onConnectionStateChange status=" + status + "->" + newState);
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 移除客户端连接设备
                isConnected=false;
                if(bleServerCallback!=null){
                    bleServerCallback.onDisconnected(device);
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected=true;
                WCHBLEPeripheralUtil.getInstance().connect(device, false);
                if(bleServerCallback!=null){
                    bleServerCallback.onConnected(device);
                }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            LogUtil.d("onServiceAdded " + service.getUuid().toString() + " " + status);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            LogUtil.d(String.format("onCharacteristicReadRequest：device name = %s, address = %s,char = %s", device.getName(), device.getAddress(), characteristic.getUuid().toString()));
            LogUtil.d(String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset));
            if(bleServerCallback!=null){
                bleServerCallback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            //super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            LogUtil.d(String.format(Locale.getDefault(),"onCharacteristicWriteRequest(%d):%s,%s,%s,%s,%s,%s,%s,%s", value.length,device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, FormatUtil.bytesToHexString(value)));
            characteristic.setValue(value);
            if(bleServerCallback!=null){
                bleServerCallback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            LogUtil.d(String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
            if(status==BluetoothGatt.GATT_SUCCESS){
                notify_state=NOTIFY_STATE.STATE_SUCCESS;
            }else {
                notify_state=NOTIFY_STATE.STATE_FAIL;
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            LogUtil.d(String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            if(bleServerCallback!=null){
                bleServerCallback.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            LogUtil.d(String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            LogUtil.d("onDescriptorWriteRequest:" + FormatUtil.bytesToHexString(value));
            // now tell the connected device that this was all successfull
            descriptor.setValue(value);
            if(bleServerCallback!=null){
                bleServerCallback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            LogUtil.d(String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            LogUtil.d(String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
            currentMTU=mtu;
            if(bleServerCallback!=null){
                bleServerCallback.onMtuChanged(device,mtu);
            }
        }
    };

    public int getMTU() {
        return currentMTU;
    }

    public boolean isConnected(){
        return isConnected;
    }
}
