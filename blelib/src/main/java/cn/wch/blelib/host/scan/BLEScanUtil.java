package cn.wch.blelib.host.scan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import cn.wch.blelib.exception.BLELibException;
import cn.wch.blelib.utils.FormatUtil;
import cn.wch.blelib.utils.LogUtil;


public class BLEScanUtil {

    private static BLEScanUtil mThis;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private Map<String,String> map;

    public static BLEScanUtil getInstance(){
        if(mThis==null){
            synchronized (BLEScanUtil.class){
                mThis=new BLEScanUtil();
            }
        }
        return mThis;
    }

    public BLEScanUtil() {
        map=new HashMap<>();
    }

    /**
     * 开始扫描，需要过滤条件
     * @param scanRuler
     * @param observer
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION})
    public synchronized void startScan(final ScanRuler scanRuler, final ScanObserver observer) throws BLELibException {
        if(BluetoothAdapter.getDefaultAdapter()==null || !BluetoothAdapter.getDefaultAdapter().isEnabled()){
            throw new BLELibException("BluetoothAdapter should be opened");
        }
        if(observer==null ){
            throw new BLELibException("ScanObserver is null");
        }
        leScanCallback=new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(device.getType()==BluetoothDevice.DEVICE_TYPE_LE && ScanMatchUtil.matchRuler(device,rssi,scanRecord,scanRuler)){
                    observer.OnScanDevice(device,rssi,scanRecord);
                }
            }
        };
        BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);
    }

    /**
     * 开始扫描,不过滤
     * @param scanCallback
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION})
    public synchronized void startScan(BluetoothAdapter.LeScanCallback scanCallback) throws BLELibException {
        if(BluetoothAdapter.getDefaultAdapter()==null || !BluetoothAdapter.getDefaultAdapter().isEnabled()){
            throw new BLELibException("BluetoothAdapter should be opened");
        }
        if(scanCallback==null ){
            throw new BLELibException("scanCallback is null");
        }
        this.leScanCallback=scanCallback;
        BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);
    }


    /**
     * 开始扫描，不会返回重复的设备
     * @param scanRuler
     * @param observer
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION})
    public synchronized void startScanNoRepeat(final ScanRuler scanRuler, final ScanObserver observer) throws BLELibException {
        if(BluetoothAdapter.getDefaultAdapter()==null || !BluetoothAdapter.getDefaultAdapter().isEnabled()){
            throw new BLELibException("BluetoothAdapter should be opened");
        }
        if(observer==null ){
            throw new BLELibException("observer is null");
        }
        leScanCallback=new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(device.getType()==BluetoothDevice.DEVICE_TYPE_LE && ScanMatchUtil.matchRuler(device,rssi,scanRecord,scanRuler)){
                    if(map.containsKey(device.getAddress())){
                        return;
                    }
                    map.put(device.getAddress(),"");
                    observer.OnScanDevice(device,rssi,scanRecord);
                }
            }
        };
        BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);
    }

    /**
     * 停止扫描
     */
    public synchronized void stopScan(){
        map.clear();
        if(leScanCallback!=null && BluetoothAdapter.getDefaultAdapter()!=null){
            BluetoothAdapter.getDefaultAdapter().stopLeScan(leScanCallback);
            LogUtil.d("stopLeScan");
        }
    }



}
