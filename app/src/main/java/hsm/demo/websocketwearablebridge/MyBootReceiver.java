package hsm.demo.websocketwearablebridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.os.BuildCompat;

public class MyBootReceiver extends BroadcastReceiver{

    final static String TAG = "MyBootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean bootCompleted;
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        if (Build.VERSION.SDK_INT >  26) {
            bootCompleted = Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);
        } else {
            bootCompleted = Intent.ACTION_BOOT_COMPLETED.equals(action);
        }
        Log.d(TAG, "bootCompleted=" + bootCompleted);
        if(bootCompleted) {
            Intent i = new Intent(context, ServiceActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(Constants.START_ON_BOOT, true);
            context.startActivity(i);
            Log.d(TAG, "started ServiceActivity");
        }
    }
}