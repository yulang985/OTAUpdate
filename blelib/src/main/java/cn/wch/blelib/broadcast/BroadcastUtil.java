package cn.wch.blelib.broadcast;


import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import cn.wch.blelib.utils.LogUtil;

public class BroadcastUtil {

    public static BroadcastData parse(@NonNull byte[] broadcastContent){
        if (broadcastContent.length == 0) {
            return null;
        }
        BroadcastData broadcastData=new BroadcastData();
        ArrayList<Unit> unitArrayList=new ArrayList<>();
        HashMap<Integer, byte[]> manufactureList=new HashMap<>();

        int off=0;
        while(off<broadcastContent.length){
            int len=broadcastContent[off] & 0xff;
            if(len==0){
                off++;
                continue;
            }
            byte type=broadcastContent[off+1];
            byte[] data=new byte[len-1];
            System.arraycopy(broadcastContent,off+2,data,0,data.length);
            off+=(len+1);
            if(type == (byte)0x01 && data.length==1) {
                broadcastData.setFlags(data[0]);
            }
            if(type == (byte)0xff){
                if(data.length<2){
                    LogUtil.d(" Manufacturer field invalid");
                }else {
                    int id = data[0] & 0xff | ((data[1] & 0x00ff) <<8);
                    byte[] mData = new byte[data.length - 2];
                    System.arraycopy(data, 2, mData, 0, mData.length);
                    manufactureList.put(id,mData);

                }
            }
            Unit unit=new Unit(len,type,data);
            unitArrayList.add(unit);
        }
        broadcastData.setManufacturerList(manufactureList);
        broadcastData.setUnits(unitArrayList);
        return broadcastData;
    }

    public static String getFlagsDescription(byte flags){
        String s="";
        if((flags & 0x01) != 0x00){
            s+="LE Limited Discoverable; ";
        }
        if((flags & 0x02) != 0x00){
            s+="LE General Discoverable; ";
        }
        if((flags & 0x04) != 0x00){
            s+="BR/EDR Not Supported; ";
        }else {
            s+="BR/EDR Supported; ";
        }
        if((flags & 0x08) != 0x00){
            s+="LE and BR/EDR Controller;";
        }
        if((flags & 0x10) != 0x00){
            s+="LE and BR/EDR host;";
        }
        return s;
    }

    public static String getManufacturerName(int id){
        return Manufacturer.getManufacturer(id);
    }
}
