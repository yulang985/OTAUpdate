package cn.wch.blelib.broadcast;

import java.util.ArrayList;
import java.util.HashMap;

public class BroadcastData {
    private byte flags=0x00;
    private HashMap<Integer, byte[]> ManufacturerList;
    private ArrayList<Unit> units;

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }


    public ArrayList<Unit> getUnits() {
        return units;
    }

    public void setUnits(ArrayList<Unit> units) {
        this.units = units;
    }

    public HashMap<Integer, byte[]> getManufacturerList() {
        return ManufacturerList;
    }

    public void setManufacturerList(HashMap<Integer, byte[]> manufacturerList) {
        ManufacturerList = manufacturerList;
    }
}
