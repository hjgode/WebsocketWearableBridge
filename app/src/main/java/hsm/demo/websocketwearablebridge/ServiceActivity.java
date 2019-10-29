package hsm.demo.websocketwearablebridge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import de.greenrobot.event.EventBus;

public class ServiceActivity extends AppCompatActivity {

    Button btnStartService, btnStopService;
    Switch switchStartServiceOnBoot;
    TextView logText;
    final static String TAG="MyServiceActivity";
    Context m_context=this;
    Boolean bStartOnBoot=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        checkPermissions();

        //grab prefs
        final SharedPreferences examplePrefs = getSharedPreferences(Constants.PREFS,0);
        final SharedPreferences.Editor editor = examplePrefs.edit();

        switchStartServiceOnBoot=findViewById(R.id.switchStartServiceOnBoot);
        switchStartServiceOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                bStartOnBoot=b;
                //commit prefs on change
                editor.putBoolean(Constants.START_ON_BOOT, bStartOnBoot);
                editor.commit();            }
        });


        bStartOnBoot = examplePrefs.getBoolean(Constants.START_ON_BOOT, true);

        switchStartServiceOnBoot.setChecked(bStartOnBoot);

        logText=findViewById(R.id.logText);
        logText.setMovementMethod(new ScrollingMovementMethod());

        btnStartService=findViewById(R.id.btnStartService);
        btnStopService=findViewById(R.id.btnStopService);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService();
            }
        });

        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService();
            }
        });

        EventBus.getDefault().register(this);

        if(bStartOnBoot){
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    startService();
                }
            }, 5000);
        }


    }

    @Override
    protected void onStop(){
        Log.d(TAG, "onStop");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void startService() {
        stopService();
        Intent serviceIntent = new Intent(this, MyWebSocketService.class);
        serviceIntent.putExtra("inputExtra", "Foreground MyWebSocketService");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, MyWebSocketService.class);
        stopService(serviceIntent);
    }

    //handles messages by websocket server
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(SocketServiceEvent  event) {
        String message=event.getMessage();
        Log.d(TAG, "on SocketServiceEvent: " + message);
        logText.append(TAG + " on SocketServiceEvent: " + message);
        //mServer.sendMessage("echo: " + message);
    }
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(MyMessageEvent  event) {
        JSONObject jsonObject =event.getMessage();
        Log.d(TAG, "on MyMessageEvent: " + event.toString());
        logText.append(TAG + " on SocketServiceEvent: " + event.toString());
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    void checkPermissions(){
        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
/*
        if (    m_context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},Constants.REQUEST_WRITE);
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH},Constants.REQUEST_BT);
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN},Constants.REQUEST_BTADMIN);
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},Constants.REQUEST_LOCATION);
            requestPermissions(new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED},Constants.REQUEST_RECEIVE_BOOT_COMPLETED);
        }
*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: " + permissions.toString() +", " +grantResults.toString());
        for (int i=0;i<grantResults.length;i++) {
            if(grantResults[i]!=PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission: " + permissions[i] + " not granted");
            }else{
                Log.d(TAG, "Permission: " + permissions[i] + " granted");
            }
        }
        return;
/*
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
            case Constants.REQUEST_RECEIVE_BOOT_COMPLETED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: REQUEST_RECEIVE_BOOT_COMPLETED");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to use REQUEST_RECEIVE_BOOT_COMPLETED", Toast.LENGTH_LONG).show();
                }
            }
        }

 */
    }

}
