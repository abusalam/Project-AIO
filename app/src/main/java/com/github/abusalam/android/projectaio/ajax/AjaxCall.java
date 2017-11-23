package com.github.abusalam.android.projectaio.ajax;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class AjaxCall extends AsyncTask<RequestData, String, JSONObject> {

  private HttpURLConnection urlConn;
  private JSONObject ResponseJSON;
  private String ResponseText;

  @Override
  protected JSONObject doInBackground(RequestData... ApiCalls) {
    publishProgress("Connecting...");
    try {
      urlConn = (HttpURLConnection) ApiCalls[0].getApiURL().openConnection();
      urlConn.setReadTimeout(10000);
      urlConn.setConnectTimeout(15000);
      urlConn.setRequestMethod("GET");
      urlConn.setDoInput(true);
      urlConn.connect();
      InputStream is = urlConn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader
        (is, "UTF-8"));

      ResponseText = "";
      String strLine;
      do {
        strLine = reader.readLine();
        ResponseText += strLine;
      } while (strLine.length() > 0);
      is.close();

      return new JSONObject(ResponseText);

    } catch (Exception e) {
      try {
        return new JSONObject("{Exception: " + e.getMessage() + "}");
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
    } finally {
      urlConn.disconnect();
    }
    return new JSONObject();
  }

  @Override
  protected void onPostExecute(JSONObject ResponseJSON) {
    this.ResponseJSON = ResponseJSON;
  }

  @Override
  protected void onProgressUpdate(String... values) {
    super.onProgressUpdate(values);
  }
}
