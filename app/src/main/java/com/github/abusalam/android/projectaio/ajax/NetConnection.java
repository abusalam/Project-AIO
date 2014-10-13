package com.github.abusalam.android.projectaio.ajax;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetConnection {

    private Context _context;

    public NetConnection(Context context) {
        this._context = context;
    }

    /**
     * Checks whether the device currently has a network connection
     */
    public boolean isDeviceConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            return false;
        }
        return false;
    }
}