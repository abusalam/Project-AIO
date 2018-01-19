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
import com.github.abusalam.android.projectaio.GoogleAuthenticator.AccountDb;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpProvider;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.TotpClock;
import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UserActivity extends ActionBarActivity {
  public static final String TAG = WorkActivity.class.getSimpleName();
  public static final String UserName = "UN";
  public static final String UID = "ID";
  public static final String Funds = "F";
  public static final String Bal = "B";
  public static final String MobileNo = "M";
  public static final String SchCount = "S";


  static final String SECRET_PREF_NAME = "mPrefSecrets";
  private SharedPreferences mPrefs;

  private JSONArray respJsonArray;
  private RequestQueue rQueue;
  private ArrayList<User> UserList;
  private ListView lvUsers;
  private ProgressBar prgBar;

  private String UserID;
  private Long SchemeID;
  private String SchemeName;

  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;
  private com.github.abusalam.android.projectaio.User mUser;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user);

    mAccountDb = new AccountDb(this);
    mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

    mPrefs = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);
    mUser = new com.github.abusalam.android.projectaio.User();
    mUser.MobileNo = mPrefs.getString(DashAIO.PREF_KEY_MOBILE, null);

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "OTP Error: " + e.getMessage()
          + " Mobile:" + mUser.MobileNo, Toast.LENGTH_LONG).show();
    }

    rQueue = VolleyAPI.getInstance(this).getRequestQueue();
    lvUsers = (ListView) findViewById(R.id.lvUsers);
    prgBar = (ProgressBar) findViewById(R.id.pbUsers);

    View v = getLayoutInflater().inflate(R.layout.user_view, null);
    lvUsers.addHeaderView(v);

    lvUsers.setOnItemClickListener(new SelectUserClickListener());
    UserList = new ArrayList<>();
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

    Log.d("Populate Users:", "Found-" + SchemeID);
    getSchemeUsers(UserID, SchemeID);
    setTitle(SchemeName + " : " + getString(R.string.title_activity_user));
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_user, menu);
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
    mAccountDb.close();
    rQueue.cancelAll(TAG);
  }

  private void getSchemeUsers(String mUID, Long SID) {

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
          + " MDN:" + mUser.MobileNo, Toast.LENGTH_SHORT).show();
      return;
    }

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "SU");
      jsonPost.put("MDN", mUID);
      jsonPost.put("OTP", mUser.pin);
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
          Log.d(TAG, "SchemeUsers: " + response.toString());
          Toast.makeText(getApplicationContext(),
            response.optString(DashAIO.KEY_STATUS),
            Toast.LENGTH_SHORT).show();
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              User mUser = new User();
              mUser.setUserID(respJsonArray.getJSONObject(i).getInt(UID));
              mUser.setUserName(respJsonArray.getJSONObject(i).optString(UserName));
              mUser.setBalance(Integer.parseInt(respJsonArray.getJSONObject(i)
                .optString(Bal).replaceAll(",", "")));
              mUser.setSanctions(Integer.parseInt(respJsonArray.getJSONObject(i)
                .optString(Funds).replaceAll(",", "")));
              mUser.setSchemes(respJsonArray.getJSONObject(i).optString(SchCount));
              mUser.setMobileNo(respJsonArray.getJSONObject(i).optString(MobileNo));
              UserList.add(mUser);
            }
            // Spinner adapter
            lvUsers.setAdapter(new UserAdapter(UserActivity.this,
              R.layout.user_view, UserList));
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
    jsonObjReq.setShouldCache(false);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
  }

  private class SelectUserClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
      if(i>0) { //Skip the List Header {i=0}
        Intent iWorks = new Intent(getApplicationContext(), WorkActivity.class);
        iWorks.putExtra(SchemeActivity.SID, SchemeID);
        iWorks.putExtra(SchemeActivity.SN, SchemeName);
        iWorks.putExtra(SchemeActivity.UID, "" + UserList.get(i - 1).getUserID());
        startActivity(iWorks);
      }
    }
  }

}
