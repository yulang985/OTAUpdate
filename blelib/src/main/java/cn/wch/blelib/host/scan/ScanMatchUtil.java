package cn.wch.blelib.host.scan;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.Arrays;

import cn.wch.blelib.utils.FormatUtil;

public class ScanMatchUtil {

    public static synchronized boolean matchRuler(BluetoothDevice device, int rssi, byte[] scanRecord, ScanRuler scanRuler){
        //LogUtil.d("check-->: "+device.getAddress()+" "+device.getName());
        if(scanRuler==null){
            return true;
        }
        if(scanRuler.union){//取条件并集,即满足一项条件就返回TRUE
            //LogUtil.d("并集");
            if (scanRuler.Name != null) {
                boolean contain = false;
                ArrayList<String> Names = new ArrayList<>(Arrays.asList(scanRuler.Name));
                for (String name : Names) {
                    if ((device.getName() == null && name == null) || (device.getName() != null && device.getName().equals(name))) {
                        contain=true;
                    }
                }
                if(contain){
                    return true;
                }
            }

            if (scanRuler.MAC != null) {
                boolean contain = false;
                ArrayList<String> MAC = new ArrayList<>(Arrays.asList(scanRuler.MAC));
                for (String mac : MAC) {
                    if (device.getAddress() != null && device.getAddress().equalsIgnoreCase(mac)) {
                        contain=true;
                    }
                }
                if(contain){
                    return true;
                }
            }

            if (scanRuler.scanRecord != null) {
                boolean contain = false;
                ArrayList<byte[]> records = scanRuler.scanRecord;
                for (byte[] b : records) {
                    if (FormatUtil.bytesToHexString(scanRecord).toUpperCase().contains(FormatUtil.bytesToHexString(b).toUpperCase())) {
                        contain = true;
                    }
                }
                if (contain) {
                    return true;
                }
            }

            if(scanRuler.rssiMin>0 || scanRuler.rssiMax>0){
                //未添加rssi过滤

            }else {
                if (rssi >= scanRuler.rssiMin && rssi <= scanRuler.rssiMax) {
                    return true;
                }
            }

            if(scanRuler.Name == null
                    && scanRuler.MAC==null
                    && scanRuler.scanRecord==null
                    && (scanRuler.rssiMin>0 || scanRuler.rssiMax>0)){
                //未添加过滤
                return true;
            }
            //皆不满足上述条件之取并集

            return false;
        }else{//条件取交集,需要满足全部条件
            if (scanRuler.Name != null) {
                boolean contain=false;
                ArrayList<String> Names = new ArrayList<>(Arrays.asList(scanRuler.Name));
                for (String name : Names) {
                    if ((device.getName() == null && name == null) || (device.getName() != null && device.getName().equals(name))) {
                        contain=true;
                    }
                }
                if(!contain){
                    return false;
                }
            }

            if (scanRuler.MAC != null) {
                boolean contain=false;
                ArrayList<String> MAC = new ArrayList<>(Arrays.asList(scanRuler.MAC));
                for (String mac : MAC) {
                    if (device.getAddress() != null && device.getAddress().equalsIgnoreCase(mac)) {
                        contain=true;
                    }
                }
                if(!contain){
                    return false;
                }
            }
            if (scanRuler.scanRecord != null) {
                boolean contain = false;
                ArrayList<byte[]> records = scanRuler.scanRecord;
                for (byte[] b : records) {

                    if (FormatUtil.bytesToHexString(scanRecord).toUpperCase().contains(FormatUtil.bytesToHexString(b).toUpperCase())) {
                        contain = true;
                    }
                }
                if (!contain) {
                    return false;
                }
            }
            if(scanRuler.rssiMin>0 || scanRuler.rssiMax>0){
                //未添加rssi过滤
            }else {
                if (rssi <= scanRuler.rssiMin || rssi >= scanRuler.rssiMax) {
                    return false;
                }
            }
            //未添加过滤或者皆满足上述条件之取交集
            return true;
        }
    }
}
