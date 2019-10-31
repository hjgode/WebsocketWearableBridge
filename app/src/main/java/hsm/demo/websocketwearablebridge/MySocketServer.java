package hsm.demo.websocketwearablebridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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

import de.greenrobot.event.EventBus;

public class MySocketServer extends WebSocketServer {

    private WebSocket mSocket;
    private static final String TAG = "MySocketServer";

    BTScannerService btScannerService=null;
    private Context m_context;
    private Handler m_handler;

    public MySocketServer(InetSocketAddress address, Context context, Handler handler) {
        super(address);
        m_context=context;
        m_handler=handler;
        Log.d(TAG, "MySocketServer Started");
        EventBus.getDefault().register(this);
    }

    public void finalize() {
        EventBus.getDefault().unregister(this);
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
                else
                    btScannerService.write(btScanCtrl.myGetBytes(btSend));
            }
        }
        else{
            //EventBus.getDefault().post(new SocketMessageEvent(message));
            EventBus.getDefault().post((new MyMessage(MyMessage.eType.infoType, MyMessage.eSource.srcWebsocketClient, message, null)));
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

    BluetoothAdapter mBluetoothAdapter=null;
    BluetoothDevice device=null;

    //handles control messages from base websocket class
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(SocketControlEvent event) {
        String message=event.getMessage();
        Log.d(TAG, "onEvent SocketControlEvent: " + message);
        //sendMessage(message);
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
        }
    }


    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(MyMessage  mMsg) {
        JSONObject jsonObject =mMsg.getJsonObject();
        Log.d(TAG, "on MyMessageEvent: " + mMsg.toString());
    }
}