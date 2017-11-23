package com.github.abusalam.android.projectaio.mpr;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.DashAIO;
import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SchemeActivity extends ActionBarActivity {
  public static final String TAG = SchemeActivity.class.getSimpleName();
  public static final String UID = "UID";
  static final String API_URL = DashAIO.API_HOST + "/apps/mpr/android/api.php";
  static final String SECRET_PREF_NAME = "mPrefSecrets";
  static final String SID = "ID";
  static final String SN = "SN";
  private SharedPreferences mPrefs;
  private JSONArray respJsonArray;
  private RequestQueue rQueue;
  private ArrayList<Scheme> SchemeList;
  private ListView lvSchemes;
  private String UserID;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scheme);

    mPrefs = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);

    rQueue = VolleyAPI.getInstance(this).getRequestQueue();
    lvSchemes = (ListView) findViewById(R.id.lvSchemes);
    progressBar = (ProgressBar) findViewById(R.id.pbSchemes);
    lvSchemes.setOnItemClickListener(new SelectSchemeClickListener());
    SchemeList = new ArrayList<>();

    Bundle mBundle = getIntent().getExtras();
    if (mBundle == null) {
      UserID = mPrefs.getString(SchemeActivity.UID, "");
    } else {
      UserID = mBundle.getString(SchemeActivity.UID);
    }

    getUserSchemes(UserID);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    //getMenuInflater().inflate(R.menu.menu_scheme, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, intent);
        return true;
      case R.id.action_settings:
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    // Save the user's current game state
    savedInstanceState.putString(UID, UserID);
    // Always call the superclass so it can save the view hierarchy state
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    // Restore value of members from saved state
    UserID = savedInstanceState.getString(UID);
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    rQueue.cancelAll(TAG);
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences.Editor ed = mPrefs.edit();
    ed.putString(UID, UserID);
    ed.apply();
  }

  private void getUserSchemes(String UID) {

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "US");
      jsonPost.put("UID", UID);
      Log.e(TAG, "UserMapID: " + UID);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          Log.e(TAG, "UserSchemes: " + response.toString());
          Toast.makeText(getApplicationContext(),
            response.optString(DashAIO.KEY_STATUS),
            Toast.LENGTH_SHORT).show();
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              Scheme mScheme = new Scheme();
              mScheme.setSchemeID(respJsonArray.getJSONObject(i).getInt("ID"));
              mScheme.setSchemeName(respJsonArray.getJSONObject(i).optString("SN"));
              SchemeList.add(mScheme);
            }
            // Spinner adapter
            lvSchemes.setAdapter(new SchemeAdapter(SchemeActivity.this,
              R.layout.scheme_view, SchemeList));
          } catch (JSONException e) {
            e.printStackTrace();
          }
          progressBar.setVisibility(View.GONE);
        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        Log.e(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
  }

  private class SelectSchemeClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

      Long SchemeID = SchemeList.get(i).getSchemeID();
      String SchemeName = SchemeList.get(i).getSchemeName();
      //Toast.makeText(getApplicationContext(),
      //        "Scheme ID: " + SchemeID
      //                + " User: " + UserID,
      //        Toast.LENGTH_SHORT).show();
      Intent iWorks = new Intent(getApplicationContext(), UserActivity.class);
      iWorks.putExtra(SchemeActivity.SID, SchemeID);
      iWorks.putExtra(SchemeActivity.SN, SchemeName);
      iWorks.putExtra(SchemeActivity.UID, UserID);
      startActivity(iWorks);
    }
  }
}
