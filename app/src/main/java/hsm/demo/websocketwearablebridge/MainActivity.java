package hsm.demo.websocketwearablebridge;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyApplication";
    private static final int SERVER_PORT = 12345;

    private MySocketServer mServer;

    Button btnServer1;

    TextView txtLog;

    Context m_context=this;
    static Handler m_handler=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();

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

        txtLog=findViewById(R.id.txtLog);
        txtLog.setMovementMethod(new ScrollingMovementMethod());

        Button btnSend=findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mServer!=null){
                    EventBus.getDefault().post(new SocketControlEvent(Constants.BT_CONNECT_MAC+"C0:EE:40:41:51:B7")); // c0:ee:40:41:51:b7
                }

            }
        });

        Button btnBeep=findViewById(R.id.btnBeep);
        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mServer!=null){
                    if(mServer.btScannerService!=null){
                        mServer.btScannerService.write(btScanCtrl.setDoBeep());
                    }
                }
            }
        });

        btnServer1=findViewById(R.id.btnServer1);
        btnServer1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mServer==null)
                    startServer();
            }
        });

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop(){
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }
    private void startServer() {
        InetAddress inetAddress = getInetAddress();
        addLog("local IP="+inetAddress.toString());
        if (inetAddress == null) {
            Log.e(TAG, "Unable to lookup IP address");
            return;
        }

        mServer = new MySocketServer(new InetSocketAddress(inetAddress.getHostAddress(), SERVER_PORT), m_context, m_handler );
        mServer.start();

        //now we can control or let the WebSocket server write to clients
        EventBus.getDefault().post(new SocketControlEvent("Hello"));
    }

    private static InetAddress getInetAddress() {
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

        return null;
    }

    //handles messages by websocket server
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(SocketMessageEvent event) {
        String message=event.getMessage();
        Log.d(TAG, "onEvent");
        mServer.sendMessage("echo: " + message);
        addLog(message);
    }

    public void addLog(final String msg){
        Log.d(TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtLog.append("\r\n"+msg);
            }
        });
    }

    void checkPermissions(){
        if (    m_context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_WRITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_WRITE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission granted: WRITE_EXTERNAL_STORAGE");
                    //do here

                } else {
                    Toast.makeText(m_context, "The app was not allowed to write in your storage", Toast.LENGTH_LONG).show();
                }
            }
            case Constants.REQUEST_BT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: BLUETOOTH");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to use Bluetooth", Toast.LENGTH_LONG).show();
                }
            }
            case Constants.REQUEST_BTADMIN: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: BLUETOOTH_ADMIN");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to manage Bluetooth", Toast.LENGTH_LONG).show();
                }
            }
            case Constants.REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: ACCESS_COARSE_LOCATION");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to use Coarse Location", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
