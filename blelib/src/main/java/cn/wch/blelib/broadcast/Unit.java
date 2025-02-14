package cn.wch.blelib.broadcast;

public class Unit {
    private int len;
    private byte type;
    private byte[] data;

    public Unit(int len, byte type, byte[] data) {
        this.len = len;
        this.type = type;
        this.data = data;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
