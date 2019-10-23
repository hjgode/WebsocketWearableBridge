package hsm.demo.websocketwearablebridge;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import de.greenrobot.event.EventBus;

public class MySocketServer extends WebSocketServer {

    private WebSocket mSocket;
    private static final String TAG = "MyApplication";

    public MySocketServer(InetSocketAddress address) {
        super(address);
        Log.d(TAG, "MySocketServer Started");
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

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "MySocketServer onMessage");
        EventBus.getDefault().post(new SocketMessageEvent(message));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.d(TAG, "MySocketServer onError");
    }

    public void sendMessage(String message) {
        mSocket.send(message);
        Log.d(TAG, "MySocketServer sendMessage");
    }
}