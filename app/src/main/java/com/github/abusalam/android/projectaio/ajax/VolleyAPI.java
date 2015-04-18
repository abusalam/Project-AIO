package com.github.abusalam.android.projectaio.ajax;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

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
