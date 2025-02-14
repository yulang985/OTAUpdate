package cn.wch.blelib.host.core.callback;

public interface RSSICallback {
    void onReadRemoteRSSI(int rssi, int status);
}
