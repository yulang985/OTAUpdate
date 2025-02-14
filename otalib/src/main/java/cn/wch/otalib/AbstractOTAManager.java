package cn.wch.otalib;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import cn.wch.blelib.exception.BLELibException;
import cn.wch.blelib.host.WCHBluetoothManager;
import cn.wch.blelib.host.core.ConnRuler;
import cn.wch.blelib.host.core.Connection;
import cn.wch.blelib.host.core.callback.ConnectCallback;
import cn.wch.blelib.host.core.callback.MTUCallback;
import cn.wch.blelib.utils.LogUtil;
import cn.wch.otalib.callback.ConCallback;
import cn.wch.otalib.entry.CurrentImageInfo;
import cn.wch.otalib.entry.ImageType;
import cn.wch.otalib.utils.FileParseUtil;
import cn.wch.otalib.utils.FormatUtil;

public abstract class AbstractOTAManager {


    //write data
    protected boolean write(BluetoothGattCharacteristic characteristic, byte[] data, int len) throws Exception {
        int write = WCHBluetoothManager.getInstance().write(characteristic, data, len);
        return write == data.length;
    }

    protected byte[] read(BluetoothGattCharacteristic characteristic) throws Exception {
        return WCHBluetoothManager.getInstance().read(characteristic,false);
    }

    //write and read data
    protected byte[] writeAndRead(BluetoothGattCharacteristic mWrite,BluetoothGattCharacteristic mRead, byte[] data, int len) throws Exception {
        if (!write(mWrite, data, len)) {
            throw new BLELibException("write fail");
        }
        return WCHBluetoothManager.getInstance().read(mRead,false);
    }

    //write and read data
    protected byte[] writeAndRead(BluetoothGattCharacteristic mWrite,BluetoothGattCharacteristic mRead, byte[] data, int len,int sleepTime) throws Exception {
        if (!write(mWrite, data, len)) {
            throw new BLELibException("write fail");
        }
        if(sleepTime>0){
            Thread.sleep(sleepTime);
        }
        return WCHBluetoothManager.getInstance().read(mRead,false);
    }

    /**
     * 扫描附近的蓝牙设备
     * @param scanCallback 扫描回调
     * @throws Exception
     */
    @SuppressLint("MissingPermission")
    public void startScan(@NonNull ScanCallback scanCallback) throws Exception{
        //no filter
        ScanFilter filter = new ScanFilter.Builder()
                .build();
        WCHBluetoothManager.getInstance().startScan(filter, scanCallback);
    }

    /**
     * 停止扫描
     */
    public void stopScan(){
        WCHBluetoothManager.getInstance().stopScan();
    }


    /**
     * 连接蓝牙设备
     * @param mac MAC地址
     * @param conCallback 连接状态回调
     */
    public void connect(String mac,@NonNull ConCallback conCallback){
        ConnRuler connRuler=new ConnRuler.Builder(mac).connectTimeout(10000).build();
        try {
            WCHBluetoothManager.getInstance().connect(connRuler, new ConnectCallback() {
                @Override
                public void OnError(String mac, Throwable t) {
                    conCallback.OnError(mac, t);
                }

                @Override
                public void OnConnecting(String mac) {
                    conCallback.OnConnecting(mac);
                }

                @Override
                public void OnConnectSuccess(String mac, Connection connection) {

                }

                @Override
                public void OnDiscoverService(String mac, List<BluetoothGattService> list) {
                    //check characteristic
                    if(checkCharacteristics(list)){
                        preSet();
                        conCallback.OnConnectSuccess(mac);
                    }else {
                        conCallback.OnInvalidDevice(mac);
                    }
                }

                @Override
                public void OnConnectTimeout(String mac) {
                    conCallback.OnConnectTimeout(mac);
                }

                @Override
                public void OnDisconnect(String mac, BluetoothDevice bluetoothDevice, int status) {
                    conCallback.OnDisconnect(mac, bluetoothDevice, status);
                }
            });
        } catch (BLELibException e) {
            e.printStackTrace();
            LogUtil.d(e.getMessage());
        }
    }

    /**
     * 断开蓝牙连接
     * @param force true 强制断开,状态回调不会接收到断开信息；false 状态回调会接收到断开信息
     * @throws Exception
     */
    public void disconnect(boolean force)throws Exception{
        WCHBluetoothManager.getInstance().disconnect(force);
    }

    /**
     * check connect state
     * @return
     */
    private boolean isConnected(){
        return WCHBluetoothManager.getInstance().isConnected();
    }

    /**
     * 检查蓝牙连接状态
     * @param mac MAC地址
     * @return
     */
    public boolean isConnected(String mac){
        return WCHBluetoothManager.getInstance().isConnected(mac);
    }

    abstract boolean checkCharacteristics(List<BluetoothGattService> list);

    abstract void preSet();

    protected void setMTU(){
        try {
            WCHBluetoothManager.getInstance().setMTU(247, new MTUCallback() {
                @Override
                public void onMTUChanged(BluetoothGatt gatt, int mtu, int status) {
                    if(status== BluetoothGatt.GATT_SUCCESS){
                        LogUtil.d("set mtu: "+mtu);
                    }else {
                        LogUtil.d("set mtu fail");
                    }
                }
            });
        } catch (BLELibException e) {
            e.printStackTrace();
        }
    }

    protected boolean checkImageIllegal(CurrentImageInfo imageInfo, ByteBuffer byteBuffer){
        if(byteBuffer.capacity()<8){
            return false;
        }
        byte[] temp=new byte[]{byteBuffer.get(4),byteBuffer.get(5),byteBuffer.get(6),byteBuffer.get(7)};
        int imageFileOffset = FormatUtil.bytesToIntLittleEndian(temp, 0);
        LogUtil.d("image offset: "+imageFileOffset);

        //检查Bin文件的offset
        //imageB的升级文件相关字段大于0x38000
        //imageA的升级文件相关字段小于0x38000
        //iamgeB升级文件大于0x4000小于0x10000
        if(imageInfo.getType()== ImageType.A && imageFileOffset>(0x00038000)){
            return true;
        }else if(imageInfo.getType()==ImageType.B && imageFileOffset<0x00038000){
            return true;
        }else if(imageInfo.getType()==ImageType.A && imageFileOffset>0x00004000 && imageFileOffset<0x00010000){
            return true;
        }else {
            return false;
        }
    }
    @RequiresPermission(anyOf = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE})
    public int getHexFileEraseAddr(@NonNull File file) throws Exception {
        if(file.getName().endsWith(".hex") || file.getName().endsWith(".HEX")){
            return FileParseUtil.parseHexFileStartAddr(file);
        }else {
            throw new Exception("only support hex file");
        }
    }

    public int getCurrentMtu()throws BLELibException{

        return WCHBluetoothManager.getInstance().getMTU();
    }

}
