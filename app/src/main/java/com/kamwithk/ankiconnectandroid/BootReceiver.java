package com.kamwithk.ankiconnectandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Double-check the action to be safe
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            // Check user preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean shouldAutostart = sharedPreferences.getBoolean("autostart_on_boot", true);

            if (shouldAutostart) {
                // Start the foreground service
                Intent serviceIntent = new Intent(context, Service.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
