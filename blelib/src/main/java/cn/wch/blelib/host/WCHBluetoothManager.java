package cn.wch.blelib.host;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanSettings.Builder;
import android.os.Build;
import android.text.TextUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import cn.wch.blelib.exception.BLELibException;
import cn.wch.blelib.host.core.BLEHostManager;
import cn.wch.blelib.host.core.ConnRuler;
import cn.wch.blelib.host.core.Connection;
import cn.wch.blelib.host.core.Connector;
import cn.wch.blelib.host.core.callback.ConnectCallback;
import cn.wch.blelib.host.core.callback.MTUCallback;
import cn.wch.blelib.host.core.callback.NotifyDataCallback;
import cn.wch.blelib.host.core.callback.PhyReadCallback;
import cn.wch.blelib.host.core.callback.PhyUpdateCallback;
import cn.wch.blelib.host.core.callback.RSSICallback;
import cn.wch.blelib.host.scan.BLEScanUtil;
import cn.wch.blelib.host.scan.BLEScanUtil2;
import cn.wch.blelib.host.scan.ScanObserver;
import cn.wch.blelib.host.scan.ScanRuler;
import cn.wch.blelib.utils.LogUtil;

public final class WCHBluetoothManager {

    private Connection connection;
    //连接以及数据传输
    private BLEHostManager bleHostManager;

    private static WCHBluetoothManager wchBluetoothManager;

    private String tempAddress;

    /**
     * 用于创建全局唯一实例
     * @return 返回全局唯一实例
     */
    public static WCHBluetoothManager getInstance() {
        if(wchBluetoothManager==null){
            synchronized (WCHBluetoothManager.class){
                wchBluetoothManager=new WCHBluetoothManager();
            }
        }
        return wchBluetoothManager;
    }

    private WCHBluetoothManager() {

    }

    /**
     * 初始化
     * @param application 上下文
     * @throws BLELibException
     */
    public void init(Application application) throws BLELibException {
        bleHostManager = BLEHostManager.getInstance(application);
        bleHostManager.init(application);
    }

    /**
     * 扫描蓝牙设备
     * @param scanRuler 扫描过滤规则
     * @param scanObserver 扫描回调
     * @throws BLELibException
     */
    @Deprecated
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION})
    public void startScan(ScanRuler scanRuler, @NonNull ScanObserver scanObserver) throws BLELibException {
        BLEScanUtil.getInstance().startScan(scanRuler,scanObserver);
    }


    /**
     * 描蓝牙设备,支持BLE5.0
     * @param scanFilter
     * @param scanCallback
     * @throws BLELibException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION})
    public void startScan(@NonNull ScanFilter scanFilter, @NonNull final ScanCallback scanCallback) throws BLELibException {
        ScanSettings scanSettings= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            scanSettings = new Builder().setLegacy(false).setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        }else {
            scanSettings = new Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        }
        BLEScanUtil2.getInstance().initScanPolicy(scanSettings,scanFilter);
        BLEScanUtil2.getInstance().startLeScan(scanCallback);
    }


    /**
     * 停止扫描
     */
    public void stopScan(){
        BLEScanUtil.getInstance().stopScan();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BLEScanUtil2.getInstance().stopLeScan();
        }
    }

    /***************************************连接相关********************************************/

    /**
     * 连接蓝牙设备
     * @param connRuler 连接规则
     * @param connectCallback 状态回调
     * @throws BLELibException
     */
    public void connect(ConnRuler connRuler, @NonNull final ConnectCallback connectCallback) throws BLELibException {
        if(connection!=null){
            throw new BLELibException("Already Connected to a device,close connection!");
        }
        if(TextUtils.isEmpty(connRuler.getMAC()) || !BluetoothAdapter.checkBluetoothAddress(connRuler.getMAC())){
            throw new BLELibException("MAC address is invalid");
        }
        if(connectCallback==null){
            throw new BLELibException("ConnectCallback is null");
        }
        if(bleHostManager==null){
            throw new BLELibException("BleHostManager is null, do you invoke method init() first?");
        }

        tempAddress=connRuler.getMAC();
        bleHostManager.asyncConnect(connRuler, new ConnectCallback() {
            @Override
            public void OnError(String mac, Throwable t) {
                connectCallback.OnError(mac,t);
            }

            @Override
            public void OnConnecting(String mac) {
                connectCallback.OnConnecting(mac);
            }

            @Override
            public void OnConnectSuccess(String mac, Connection mConnection) {
                connection=mConnection;
                connectCallback.OnConnectSuccess(mac, mConnection);
            }

            @Override
            public void OnDiscoverService(String mac, List<BluetoothGattService> list) {
                connectCallback.OnDiscoverService(mac,list);
            }

            @Override
            public void OnConnectTimeout(String mac) {
                connection=null;
                try {
                    disconnect(true);
                } catch (BLELibException e) {
                    e.printStackTrace();
                }
                connectCallback.OnConnectTimeout(mac);
            }

            @Override
            public void OnDisconnect(String mac, BluetoothDevice bluetoothDevice, int status) {
                connection=null;
                connectCallback.OnDisconnect(mac,bluetoothDevice,status);
            }
        });

    }
    /**
     * 主动断开连接
     * @param force 是否强制断开：true 强制断开释放资源，可能不会有连接状态断开的回调；false 正常断开，一般会产生连接状态回调
     */
    public void disconnect(boolean force) throws BLELibException {
        LogUtil.d("disconnect:"+force);
        if(connection==null){
            return;
        }
        String mac = connection.MAC;
        if(TextUtils.isEmpty(mac) || !BluetoothAdapter.checkBluetoothAddress(mac)){
            throw new BLELibException("MAC address is invalid");
        }
        if(bleHostManager==null){
            throw new BLELibException("BleHostManager is null, do you invoke method init() first?");
        }
        bleHostManager.disconnect(mac);
        if(force){
            bleHostManager.close(mac);
            connection=null;
        }
    }

    /***************************************通信相关********************************************/

    /**
     * 发送数据
     * @param mWrite 特征值
     * @param data 待发送数据
     * @param length 待发送数据的长度
     * @return 该值为负 发送出错；不为负 发送成功的数据长度
     * @throws BLELibException
     */
    public int write(BluetoothGattCharacteristic mWrite, byte[] data, int length) throws BLELibException {
        int total=0;
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        Connector connector = connection.getConnector();
        if(connector==null || mWrite==null ||  length<0){
            throw new BLELibException("Connector is null,or characteristic is null,or length is negative");
        }
        if((mWrite.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE)==0 && (mWrite.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)==0){
            throw new BLELibException("This characteristic doesn't has WRITE/WRITE_NO_RESPONSE Property");
        }
        if(data.length==0 || length==0){
            return 0;
        }
        int packetLen=connector.getMax_packet();
        LogUtil.d("current max pack length:"+packetLen);
        int fullCount=Math.min(length,data.length)/packetLen;
        for(int i=0;i<fullCount;i++){
            byte[] tmp=new byte[packetLen];
            System.arraycopy(data,i*packetLen,tmp,0,packetLen);
            if(!writeSinglePacket( connector,mWrite,tmp)) {
                return total;
            }
            total+=tmp.length;
            if(i==(fullCount-1) && data.length%packetLen==0){
                break;
            }

        }
        int res = Math.min(length, data.length) % packetLen;
        if(res!=0) {
            byte[] tmp=new byte[res];
            System.arraycopy(data, fullCount * packetLen, tmp, 0, tmp.length);
            if (!writeSinglePacket(connector,mWrite, tmp)) {
                return total;
            }
            //LogUtil.d("final write "+tmp.length);
            total+=tmp.length;
        }
        return total;
    }

    private boolean writeSinglePacket(Connector connector, BluetoothGattCharacteristic characteristic, byte[] tmp){
        if(connector==null){
            return false;
        }
        return connector.syncWriteCharacteristic(characteristic, tmp);
    }

    /**
     * 用于单包发送数据，如果发送长度大于最大包长，会抛出异常。
     * @param writeCharacteristic 特征值
     * @param data 待发送数据
     * @param length 待发送数据的长度
     * @return false 发送失败;true 发送成功
     * @throws BLELibException
     */
    public boolean writePacket(@NonNull BluetoothGattCharacteristic writeCharacteristic, @NonNull byte[] data, int length)throws BLELibException{
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        Connector connector = connection.getConnector();
        if(connector==null || writeCharacteristic==null ||  length<0){
            throw new BLELibException("Connector is null,or characteristic is null,or length is negative");
        }
        if((writeCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE)==0 && (writeCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)==0){
            throw new BLELibException("This characteristic doesn't has WRITE/WRITE_NO_RESPONSE Property");
        }
        if(data.length==0 || length==0){
            return true;
        }
        int packetLen=connector.getMax_packet();
        LogUtil.d("当前最大包长："+packetLen);
        int fullCount=Math.min(length,data.length);
        if(fullCount>packetLen){
            throw new BLELibException("In this method,you should't write data which length more than maximum package length "+packetLen);
        }
        byte[] tmp=new byte[fullCount];
        System.arraycopy(data, 0, tmp, 0, tmp.length);
        return writeSinglePacket(connector,writeCharacteristic, tmp);
    }


    /**
     * 读取数据
     * @param readCharacteristic 特征值
     * @param single false 未读到数据时，多次尝试;true 单次读取数据
     * @return 读取到的数据内容
     * @throws BLELibException
     */
    public byte[] read(@NonNull BluetoothGattCharacteristic readCharacteristic,boolean single) throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        return connection.read(readCharacteristic,single);
    }

    /**
     *检查蓝牙设备是否处于连接状态
     * @param mac mac地址
     * @return false 未连接;true 已连接
     */
    public boolean isConnected(String mac){

        return  bleHostManager!=null && bleHostManager.isConnected(mac);
    }

    public boolean isConnected(){

        return  bleHostManager!=null && tempAddress!=null && bleHostManager.isConnected(tempAddress);
    }

    /***************************************通知相关********************************************/
    /**
     * 打开通知
     * @param notifyCharacteristic 特征值
     * @param notifyDataCallback 通知数据回调,null 可以取消回调
     * @return true 操作成功；false 操作失败
     * @throws BLELibException
     */
    public boolean openNotify(@NonNull BluetoothGattCharacteristic notifyCharacteristic, NotifyDataCallback notifyDataCallback) throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        return connection.setNotifyListener(notifyCharacteristic,notifyDataCallback,true);
    }

    /**
     * 设置通知回调
     * @param notifyCharacteristic 特征值
     * @param notifyDataCallback 通知数据回调,null 可以取消回调
     * @return true 操作成功；false 操作失败
     * @throws BLELibException
     */
    public void setNotifyListener(@NonNull BluetoothGattCharacteristic notifyCharacteristic, NotifyDataCallback notifyDataCallback) throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        connection.setNotifyListener(notifyCharacteristic,notifyDataCallback);
    }

    /**
     * 获取通知状态
     * @param notifyCharacteristic 特征值
     * @return true 通知已经打开；false 通知已经关闭
     * @throws BLELibException
     */
    public boolean getNotifyState(@NonNull BluetoothGattCharacteristic notifyCharacteristic) throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        return connection.getNotifyState(notifyCharacteristic);
    }


    /**
     * 关闭通知
     * @param notifyCharacteristic 特征值
     * @return true 操作成功；false 操作失败
     * @throws BLELibException
     */
    public boolean closeNotify(@NonNull BluetoothGattCharacteristic notifyCharacteristic) throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        return connection.enableNotify(false,notifyCharacteristic);
    }



    /***************************************MTU相关********************************************/
    /**
     * 设置MTU
     * @param mtu 希望设置的MTU大小
     * @param mtuCallback MTU状态回调
     * @throws BLELibException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setMTU(int mtu, MTUCallback mtuCallback)throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        if(mtu<23){
            throw new BLELibException("MTU should more than 23");
        }
        if(mtuCallback==null){
            throw new BLELibException("MTUCallback is null");
        }
        connection.setMtu(mtu,mtuCallback);
    }

    /**
     * 获取当前MTU
     * @return 返回当前MTU
     * @throws BLELibException
     */
    public int getMTU()throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        return connection.getMtu();
    }

    /***************************************RSSI相关********************************************/


    /**
     * 设置RSSI上报通知
     * @param rssiCallback rssi上报回调
     */
    public void setRSSINotify(RSSICallback rssiCallback){
        if(connection!=null){
            connection.setRSSICallback(rssiCallback);
        }
    }

    /**
     *用于连接设备后，主动读取RSSI值。需配合setRSSINotify ()函数使用
     * @return true 操作成功；false 操作失败
     */
    public boolean readRSSI(){
        return connection!=null && connection.readRssi();
    }


    /**************************************PHY****************************************/
    /**
     * Read the current transmitter PHY
     * @param phyReadCallback
     * @throws BLELibException
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void readPhy(PhyReadCallback phyReadCallback)throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        connection.readPhy(phyReadCallback);
    }

    /**
     * Set the preferred connection PHY for this app
     * @param txPhy preferred transmitter PHY. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     * BluetoothDevice#PHY_LE_CODED_MASK}.
     * @param rxPhy preferred receiver PHY. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     * BluetoothDevice#PHY_LE_CODED_MASK}.
     * @param phyOptions preferred coding to use when transmitting on the LE Coded PHY. Can be one
     * of {@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}, {@link BluetoothDevice#PHY_OPTION_S2} or
     * {@link BluetoothDevice#PHY_OPTION_S8}
     * @param phyUpdateCallback
     * @throws BLELibException
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions, PhyUpdateCallback phyUpdateCallback)throws BLELibException {
        if(connection==null){
            throw new BLELibException("Connection is null,BT is disconnected");
        }
        connection.setPreferredPhy(txPhy, rxPhy, phyOptions, phyUpdateCallback);
    }


}
