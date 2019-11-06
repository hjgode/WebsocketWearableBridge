package hsm.demo.websocketwearablebridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import android.os.Handler;

public class MySocketServer extends WebSocketServer {

    private WebSocket mSocket;
    private static final String TAG = "MySocketServer";

    BTScannerService btScannerService=null;
    private Context m_context;
    private Handler m_handler;

    BluetoothAdapter mBluetoothAdapter=null;
    BluetoothDevice device=null;

    public MySocketServer(InetSocketAddress address, Context context, Handler handler) {
        super(address);
        m_context=context;
        m_handler=handler;
        Log.d(TAG, "MySocketServer Started");
    }

    //Destructor
    public void finalize() {
        btScannerService.stop();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        mSocket = conn;
        Log.d(TAG, "MySocketServer onOpen");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "MySocketServer onClose");
    }

    /// Messages received by a WebSocket client (HTML browser web page)
    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "MySocketServer onMessage: " + message);
        if(message.startsWith(Constants.BT_CONNECT_MAC)){
            String sMac=message.substring(Constants.BT_CONNECT_MAC.length());
            btScannerService=new BTScannerService(m_context, m_handler);
            // Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //find device
            device = mBluetoothAdapter.getRemoteDevice(sMac);
            //connect to BT device
            btScannerService.connect(device);
            Log.d(TAG, "scanner service state="+btScannerService.getState());
        }else if(message.startsWith(Constants.BT_DISCONNECT)){
            btScannerService.stop();
        }
        else if(message.startsWith(Constants.BT_SEND))
        {
            if (btScannerService.getState() == Constants.STATE_CONNECTED) {
                String btSend=message.substring(Constants.BT_SEND.length());
                if(btSend=="BEEP")
                    btScannerService.write(btScanCtrl.setDoBeep());
                else if(btSend.startsWith(Constants.BT_DECHDR)){
                    String s=btSend.substring(Constants.BT_DECHDR.length());
                    byte[] bdechdr;
                    if(s.startsWith("1"))
                        bdechdr=btScanCtrl.setDecHeaderSetting(true);
                    else
                        bdechdr=btScanCtrl.setDecHeaderSetting(false);
                    btScannerService.write(bdechdr);
                }
                else
                    btScannerService.write(btScanCtrl.myGetBytes(btSend));
            }
        }
        else{
            Message msg=new Message();
            msg.what=Constants.MESSAGE_ONMESSSAGE;
            Bundle bundle=new Bundle();
            bundle.putString("MESSAGE", message);
            msg.setData(bundle);
            m_handler.sendMessage(msg);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        Log.d(TAG, "MySocketServer onError: "+ex.getMessage());
    }

    public void sendMessage(String message) {
        if(mSocket!=null) {
            mSocket.send(message);
            Log.d(TAG, "MySocketServer sendMessage: "+message);
        }
        else {
            Log.d(TAG, "MySocketServer sendMessage: " + "socket not connected");
        }
    }
    public void sendMessage(byte[] message) {
        String sMsg = new String(message, StandardCharsets.UTF_8);
        if(mSocket!=null) {
            mSocket.send(sMsg);
            Log.d(TAG, "MySocketServer sendMessage: "+sMsg);
        }
        else {
            Log.d(TAG, "MySocketServer sendMessage: " + "socket not connected");
        }

    }

    public void onEvent(){

    }

}