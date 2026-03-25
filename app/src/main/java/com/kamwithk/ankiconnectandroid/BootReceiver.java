package com.kamwithk.ankiconnectandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Double-check the action to be safe
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            // Start the foreground service
            Intent serviceIntent = new Intent(context, Service.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
