package cn.wch.blelib.host.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;

import cn.wch.blelib.exception.BLELibException;
import cn.wch.blelib.host.core.callback.ConnectCallback;
import cn.wch.blelib.utils.BLEUtil;

public class ConnPresent {
    private Context context;
    private static ConnPresent mThis;
    private Connection con=null;
    private volatile boolean isConnect=false;
    private HashMap<String, Connection> connectionHashMap=new HashMap<>();
    private HashMap<String, Connector> connectorHashMap=new HashMap<>();
    private ConnPresent(Context context) {
        this.context=context;
    }

    public static ConnPresent getInstance(Context context){
        if(mThis==null){
            synchronized (ConnPresent.class){
                mThis=new ConnPresent(context);
            }
        }
        return mThis;
    }

    public void asyncConnect(final ConnRuler connRuler,final ConnectCallback callback) throws BLELibException {
        final String label = getMacFromRuler(connRuler);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Connector connector = new Connector(context,label, connRuler, new ConnectCallback() {
                    @Override
                    public void OnError(String mac,Throwable t) {
                        if(callback!=null) {
                            callback.OnError(mac, t);
                        }
                    }

                    @Override
                    public void OnConnecting(String mac) {
                        if(callback!=null) {
                            callback.OnConnecting(mac);
                        }
                    }

                    @Override
                    public void OnConnectSuccess(String mac, Connection connection) {
                        connectionHashMap.remove(label);
                        connectionHashMap.put(label, connection);
                        if(callback!=null) {
                            callback.OnConnectSuccess(mac, connection);
                        }
                    }

                    @Override
                    public void OnDiscoverService(String mac,List<BluetoothGattService> list) {
                        if(callback!=null) {
                            callback.OnDiscoverService(mac, list);
                        }
                    }

                    @Override
                    public void OnConnectTimeout(String mac) {
                        try {
                            close(mac);
                        } catch (BLELibException e) {
                            e.printStackTrace();
                        }
                        if(callback!=null) {
                            callback.OnConnectTimeout(mac);
                        }
                    }

                    @Override
                    public void OnDisconnect(String mac,BluetoothDevice bluetoothDevice, int status) {
                        try {
                            close(mac);
                        } catch (BLELibException e) {
                            e.printStackTrace();
                        }
                        if(callback!=null) {
                            callback.OnDisconnect(mac, bluetoothDevice, status);
                        }
                    }
                });
                connectorHashMap.put(label,connector);
            }
        }).start();
    }

    public void disconnect(String mac)throws BLELibException {
        if(mac==null){
            throw new BLELibException("mac is null");
        }
        if(!connectionHashMap.containsKey(mac) ){
            throw new BLELibException("ConnectionHashMap do not contain this mac");
        }else{
            Connection connection=connectionHashMap.get(mac);
            connection.disconnect();
            connectionHashMap.remove(mac);
        }
    }

    public void close(String mac)throws BLELibException {
        if(mac==null){
            throw new BLELibException("mac is null");
        }
        if(!connectorHashMap.containsKey(mac)){
            //throw new BLELibException("ConnectorHashMap do not contain this mac");
        }else {
            Connector connector=connectorHashMap.get(mac);
            if (connector != null) {
                connector.close();
                connector=null;
            }
            connectorHashMap.remove(mac);
        }
        if(!connectionHashMap.containsKey(mac) ){
            //throw new BLELibException("ConnectionHashMap do not contain this mac");
        }else{
            Connection connection=connectionHashMap.get(mac);
            if (connection != null) {
                connection.close();
                connection=null;
            }
            connectionHashMap.remove(mac);
        }
    }

    public void closeAll(){
        if(connectionHashMap.isEmpty()){
            return;
        }
        for (String s:connectionHashMap.keySet()) {
            Connection connection = connectionHashMap.get(s);
            if (connection != null) {
                connection.close();
                connection=null;
            }
        }
        connectionHashMap.clear();
    }

    public boolean isConnected(String mac){
        if(mac==null){
            return false;
        }
        if(!connectionHashMap.containsKey(mac)){
            return false;
        }else{
            Connection connection=connectionHashMap.get(mac);
            return connection.isConnected();
        }
    }

    public Connection getConnection(String mac){
        if(TextUtils.isEmpty(mac) || !BLEUtil.isValidMac(mac)){
            return null;
        }
        return connectionHashMap.get(mac);
    }

    public HashMap<String, Connection> getConnDevice(){
        return connectionHashMap;
    }

    public static boolean isValidMac(String macStr) {
        if (macStr == null || macStr.equals("")) {
            return false;
        }
        String macAddressRule = "([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}";
        // 这是真正的MAC地址；正则表达式；
        if (macStr.matches(macAddressRule)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidRulerSimple(ConnRuler connRuler){
        if(connRuler==null){
            return false;
        }
        if(connRuler.getMAC()==null ){
            return false;
        }else {
            if(!isValidMac(connRuler.getMAC())){
                return false;
            }
            return true;
        }
    }

    private static void checkRuler(ConnRuler connRuler)throws BLELibException {
        if(!isValidRulerSimple(connRuler)){
            throw new BLELibException("ConnRuler MAC  is invalid");
        }
        if(connRuler.getConnectTimeout()<=0){
            throw new BLELibException("ConnectTimeout should more than 0 ");
        }


    }

    public static String getMacFromRuler(ConnRuler connRuler) throws BLELibException {
        checkRuler(connRuler);
        return connRuler.getMAC();
    }


}
