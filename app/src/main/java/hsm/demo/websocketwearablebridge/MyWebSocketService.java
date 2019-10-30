package hsm.demo.websocketwearablebridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import de.greenrobot.event.EventBus;

import static android.app.Service.START_NOT_STICKY;
import static hsm.demo.websocketwearablebridge.MainActivity.m_handler;

public class MyWebSocketService extends Service {
    public static final String CHANNEL_ID = "WebSocketServiceChannel";
    public static final String TAG = "MyWebSocketService";
    MySocketServer mServer;
    Context m_context=this;
    private static final int SERVER_PORT = 12345;
    Handler m_handler=null;

    @Override
    public void onCreate() {
        super.onCreate();
        //handles messages received by websocket server client
        m_handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                addLog(msg.toString());

                Bundle bundle=msg.getData();
                if (msg.what==Constants.MESSAGE_DEVICE_NAME){
                    if(bundle.containsKey(Constants.DEVICE_NAME)) {
                        String sDevice = bundle.get(Constants.DEVICE_NAME).toString();
                        if (sDevice != null) {
                            addLog("connected to " + sDevice);
                            if(mServer!=null) {
                                mServer.sendMessage(btScanCtrl.setDoBeep());
                            }
                        }
                    }
                }else if (msg.what==Constants.MESSAGE_READ) {
                    if(mServer!=null) {
                        byte[] bData=msg.getData().getByteArray("DATA");
                        try {
                            String sData = new String(bData, "UTF-8");
                            if(sData.contains("\u0006") || sData.contains("\u0015")) { // 06=ACK, 21(0x15)=NAK
                                mServer.sendMessage(sData);
                            }else{
                                mServer.sendMessage("SCAN_DATA" + sData);
                            }
                        }catch (UnsupportedEncodingException e){

                        }
                    }
                }
            }
        };
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new SocketServiceEvent("Service onCreate() done"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EventBus.getDefault().post(new SocketServiceEvent("Service onStartCommand(): " + intent.toString() +", start ID=" + startId));

        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, ServiceActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Websocket Server Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        EventBus.getDefault().post(new SocketServiceEvent("Service startForeground"));
        startForeground(1, notification);

        //do heavy work on a background thread
        startServer();

        //stopSelf();

        return START_NOT_STICKY;
    }

    private void startServer() {
        //block until we have an IP address
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                InetAddress inetAddress1;
                do{
                    inetAddress1 = getInetAddress();
                    try {
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        Log.d(TAG, "startServer getInetAddress interrupted");
                    }
                }while (inetAddress1==null);
            }
        };
        runnable.run(); //block until valid IP
        final InetAddress inetAddress=getInetAddress();
        if (inetAddress == null) {
            Log.e(TAG, "Unable to lookup IP address");
            return;
        }else{
            addLog("local IP="+inetAddress.toString());
        }

        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServer starting..."));
        mServer = new MySocketServer(new InetSocketAddress(inetAddress.getHostAddress(), SERVER_PORT), m_context, m_handler );
        mServer.start();
        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServer started."));
    }

    @Override
    public void onDestroy() {
        if(mServer.btScannerService!=null)
            mServer.btScannerService.stop();
        try {
            mServer.stop();
        }
        catch (InterruptedException e){}
        catch (IOException e){}
        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServerService onDestroy"));
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServerService onBind"));
        return null;
    }

    private void createNotificationChannel() {
        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServerService createNotificationChannel()"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    private static InetAddress getInetAddress() {
        try{
        InetAddress localHost= InetAddress.getByAddress(new byte[]{127,0,0,1});// getLoopbackAddress();// ByAddress(new byte[]{127,0,0,1});
        return  localHost;}catch (Exception e){}
/*
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = (NetworkInterface) en.nextElement();

                for (Enumeration enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "Error getting the network interface information");
        }
 */
        return null;


    }
    void addLog(String m){
        Log.d(TAG, m);
        EventBus.getDefault().post(new SocketServiceEvent ("WebSocketServerService: " +m ));
    }
    //handles messages by websocket server
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(SocketControlEvent event) {
        String message=event.getMessage();
        Log.d(TAG, "on SocketControlEvent: " + message);
        //mServer.sendMessage("echo: " + message);
        EventBus.getDefault().post(new MyMessage(MyMessage.eType.infoType, MyMessage.eSource.srcWebsocketServer, event.getMessage(),null));
    }
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(MyMessage  mMsg) {
        JSONObject jsonObject =mMsg.getJsonObject();
        Log.d(TAG, "on MyMessageEvent: " + mMsg.toString());
    }
}