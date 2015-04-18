package com.github.abusalam.android.projectaio.ajax;

import java.net.URL;
import java.util.HashMap;

public class RequestData {
  private URL ApiURL;
  private String CallAPI;
  private HashMap Params;

  public RequestData(URL apiURL, String callAPI, HashMap params) {
    ApiURL = apiURL;
    CallAPI = callAPI;
    Params = params;
  }

  public URL getApiURL() {
    return ApiURL;
  }

  public void setApiURL(URL apiURL) {
    ApiURL = apiURL;
  }

  public String getCallAPI() {
    return CallAPI;
  }

  public void setCallAPI(String callAPI) {
    CallAPI = callAPI;
  }

  public HashMap getParams() {
    return Params;
  }

  public void setParams(HashMap params) {
    Params = params;
  }
}
