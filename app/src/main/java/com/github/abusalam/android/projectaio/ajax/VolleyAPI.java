package com.github.abusalam.android.projectaio.ajax;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class VolleyAPI {

    public static final String TAG = VolleyAPI.class.getSimpleName();
    private static VolleyAPI instance;
    private RequestQueue requestQueue;

    private VolleyAPI(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public static VolleyAPI getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyAPI(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

}
