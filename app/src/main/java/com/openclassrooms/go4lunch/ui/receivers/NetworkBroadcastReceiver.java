package com.openclassrooms.go4lunch.ui.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.openclassrooms.go4lunch.ui.activities.MainActivityCallback;

/**
 * This class is used to detect any "network change" event and send the information to a {@link MainActivityCallback} callback
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {

    private final MainActivityCallback callback;

    public NetworkBroadcastReceiver(MainActivityCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        callback.updateNetworkInfoBarDisplay(networkInfo != null);
    }
}