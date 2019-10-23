package hsm.demo.websocketwearablebridge;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

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

    TextView txtLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLog=findViewById(R.id.txtLog);
        txtLog.setMovementMethod(new ScrollingMovementMethod());

        startServer();
        EventBus.getDefault().register(this);
    }
    private void startServer() {
        InetAddress inetAddress = getInetAddress();
        addLog("local IP="+inetAddress.toString());
        if (inetAddress == null) {
            Log.e(TAG, "Unable to lookup IP address");
            return;
        }

        mServer = new MySocketServer(new InetSocketAddress(inetAddress.getHostAddress(), SERVER_PORT));
        mServer.start();
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
}
