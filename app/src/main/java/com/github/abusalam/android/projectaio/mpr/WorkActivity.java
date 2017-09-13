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

public class WorkActivity extends ActionBarActivity {
  public static final String TAG = WorkActivity.class.getSimpleName();
  public static final String WorkID = "WorkID";
  public static final String UID = "UserMapID";
  public static final String WorkName = "Work";
  public static final String Progress = "Progress";
  public static final String Funds = "Funds";
  public static final String Bal = "Balance";
  public static final String WR = "WorkRemarks";
  public static final String Rem = "Remarks";

  static final String SECRET_PREF_NAME = "mPrefSecrets";
  private SharedPreferences mPrefs;

  private JSONArray respJsonArray;
  private RequestQueue rQueue;
  private ArrayList<Work> WorkList;
  private ListView lvWorks;
  private ProgressBar prgBar;

  private String UserID;
  private Long SchemeID;
  private String SchemeName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_work);

    mPrefs = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);
    rQueue = VolleyAPI.getInstance(this).getRequestQueue();
    lvWorks = (ListView) findViewById(R.id.lvWorks);
    prgBar = (ProgressBar) findViewById(R.id.pbWorks);

    lvWorks.setOnItemClickListener(new SelectWorkClickListener());
    WorkList = new ArrayList<>();

    Bundle mBundle = getIntent().getExtras();
    if (mBundle == null) {
      UserID = mPrefs.getString(SchemeActivity.UID, "");
      SchemeID = mPrefs.getLong(SchemeActivity.SID, 0);
      SchemeName = mPrefs.getString(SchemeActivity.SN, "");
    } else {
      UserID = mBundle.getString(SchemeActivity.UID);
      SchemeID = mBundle.getLong(SchemeActivity.SID);
      SchemeName = mBundle.getString(SchemeActivity.SN);
    }

    Log.d("Populate Works:", "Found-" + UserID);
    getUserWorks(UserID, SchemeID);
    setTitle(SchemeName + " : " + getString(R.string.title_activity_work));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_work, menu);
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
    // Save the user's current state
    savedInstanceState.putString(SchemeActivity.UID, UserID);
    savedInstanceState.putLong(SchemeActivity.SID, SchemeID);
    savedInstanceState.putString(SchemeActivity.SN, SchemeName);
    // Always call the superclass so it can save the view hierarchy state
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    UserID = savedInstanceState.getString(SchemeActivity.UID);
    SchemeID = savedInstanceState.getLong(SchemeActivity.SID);
    SchemeName = savedInstanceState.getString(SchemeActivity.SN);
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences.Editor ed = mPrefs.edit();
    ed.putString(SchemeActivity.UID, UserID);
    ed.putLong(SchemeActivity.SID, SchemeID);
    ed.putString(SchemeActivity.SN, SchemeName);
    ed.apply();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    rQueue.cancelAll(TAG);
  }

  private void getUserWorks(String mUID, Long SID) {

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "UW");
      jsonPost.put("UID", mUID);
      jsonPost.put("SID", SID);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      SchemeActivity.API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          Log.d(TAG, "UserSchemes: " + response.toString());
          Toast.makeText(getApplicationContext(),
            response.optString(DashAIO.KEY_STATUS),
            Toast.LENGTH_SHORT).show();
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              Work mWork = new Work();
              mWork.setWorkID(respJsonArray.getJSONObject(i).getInt(WorkID));
              mWork.setUserMapID(respJsonArray.getJSONObject(i).getInt(UID));
              mWork.setWorkName(respJsonArray.getJSONObject(i).optString(WorkName));
              mWork.setBalance(Integer.parseInt(respJsonArray.getJSONObject(i)
                .optString(Bal).replaceAll(",", "")));
              mWork.setFunds(respJsonArray.getJSONObject(i).optString(Funds));
              mWork.setProgress(respJsonArray.getJSONObject(i).optInt(Progress));
              mWork.setWorkRemarks(respJsonArray.getJSONObject(i).optString(WR));
              mWork.setRemarks(respJsonArray.getJSONObject(i).optString(Rem));
              WorkList.add(mWork);
            }
            // Spinner adapter
            lvWorks.setAdapter(new WorkAdapter(WorkActivity.this,
              R.layout.work_view, WorkList));
          } catch (JSONException e) {
            e.printStackTrace();
          }
          prgBar.setVisibility(View.GONE);

        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        Log.d(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
  }

  private class SelectWorkClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

      Toast.makeText(getApplicationContext(),
        "Work ID: " + WorkList.get(i).getWorkID(),
        Toast.LENGTH_SHORT).show();
      Intent iPrg = new Intent(getApplicationContext(), ProgressActivity.class);
      iPrg.putExtra(WorkName, WorkList.get(i));
      iPrg.putExtra(ProgressActivity.DYN_TITLE, SchemeName
        + " : " + WorkList.get(i).getWorkID());
      startActivity(iPrg);
    }
  }
}
