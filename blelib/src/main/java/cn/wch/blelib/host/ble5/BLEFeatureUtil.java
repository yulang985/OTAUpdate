package cn.wch.blelib.host.ble5;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class BLEFeatureUtil {

    public static boolean isBleSupported(Context context){
        PackageManager packageManager=context.getPackageManager();
        return BluetoothAdapter.getDefaultAdapter()!=null && packageManager!=null && packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static boolean isLeExtendedAdvertisingSupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isLeExtendedAdvertisingSupported();
        }
        return false;
    }

    public static boolean isLe2MPhySupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isLe2MPhySupported();
        }
        return false;
    }

    public static boolean isLeCodedPhySupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isLeCodedPhySupported();
        }
        return false;
    }

    public static boolean isMultipleAdvertisementSupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported();
        }
        return false;
    }

    public static boolean isOffloadedFilteringSupported(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isOffloadedFilteringSupported();
        }
        return false;
    }

    public static boolean isOffloadedScanBatchingSupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isOffloadedScanBatchingSupported();
        }
        return false;
    }

    public static boolean isLePeriodicAdvertisingSupported(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().isLePeriodicAdvertisingSupported();
        }
        return false;
    }

    public static int getLeMaximumAdvertisingDataLength(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && BluetoothAdapter.getDefaultAdapter()!=null) {
            return BluetoothAdapter.getDefaultAdapter().getLeMaximumAdvertisingDataLength();
        }
        return -1;
    }

    public static boolean isPeripheralSupported(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && BluetoothAdapter.getDefaultAdapter()!=null
            && BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner()!=null){
            return true;
        }
        return false;
    }
}
