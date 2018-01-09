package com.github.abusalam.android.projectaio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.AccountDb;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpProvider;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.TotpClock;
import com.github.abusalam.android.projectaio.ajax.NetConnection;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.mpr.SchemeActivity;
import com.github.abusalam.android.projectaio.sms.GroupActivity;
import com.github.abusalam.android.projectaio.sms.GroupSMS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DashAIO extends ActionBarActivity
  implements NavigationDrawerFragment.NavigationDrawerCallbacks {

  public static final String TAG = DashAIO.class.getSimpleName();
  public static final String API_HOST = "http://10.247.71.16";
  static final String API_URL = API_HOST + "/apps/android/api.php";
  public static final String KEY_SENT_ON = "ST";
  public static final String KEY_STATUS = "MSG";
  public static final String KEY_API = "API";
  public static final String SECRET_PREF_NAME = "mPrefSecrets";
  public static final String PREF_KEY_UserMapID = "UserMapID";

  public static final String PREF_KEY_MOBILE = "pref_mobile";
  static final int UPDATE_PROFILE_REQUEST = 0;
  static final String PREF_KEY_NAME = "pref_display_name";
  static final String PREF_KEY_POST = "pref_designation";
  static final String PREF_KEY_EMAIL = "pref_email";

  private User mUser;
  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;
  private RequestQueue rQueue;
  private TextView tvMsg;
  private SeekBar sbUserMapID;

  private Boolean oneTime;

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private NavigationDrawerFragment mNavigationDrawerFragment;

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dash_aio);

    mAccountDb = new AccountDb(this);
    mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

    SharedPreferences mInSecurePrefs;
    mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

    mUser = new User();
    mUser.MobileNo = mInSecurePrefs.getString(DashAIO.PREF_KEY_MOBILE, null);
    rQueue = VolleyAPI.getInstance(this).getRequestQueue();

    mNavigationDrawerFragment = (NavigationDrawerFragment)
      getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(
      R.id.navigation_drawer,
      (DrawerLayout) findViewById(R.id.drawer_layout));

    NetConnection IC = new NetConnection(getApplicationContext());
    TextView tvNetConn = (TextView) findViewById(R.id.tvNetConn);
    TextView tvUserName = (TextView) findViewById(R.id.tvNavUserName);
    TextView tvDesg = (TextView) findViewById(R.id.tvNavDesg);
    TextView tvEMail = (TextView) findViewById(R.id.tvNavEmail);
    TextView tvMobile = (TextView) findViewById(R.id.tvNavMobile);
    sbUserMapID = (SeekBar) findViewById(R.id.sbUserMapID);

    tvMsg = (TextView) findViewById(R.id.tvMsg);

    SharedPreferences settings = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);
    tvUserName.setText(settings.getString(PREF_KEY_NAME, ""));
    tvDesg.setText(settings.getString(PREF_KEY_POST, ""));
    tvEMail.setText(settings.getString(PREF_KEY_EMAIL, ""));
    tvMobile.setText(settings.getString(PREF_KEY_MOBILE, ""));
    mUser.UserMapID = settings.getString(PREF_KEY_UserMapID, "Not Available");

    if (IC.isDeviceConnected()) {
      tvNetConn.setVisibility(View.GONE);
      //showAttendance();
    } else {
      tvNetConn.setText(getString(R.string.NC));
      tvMsg.setText("ID: " + mUser.UserMapID);
    }

    TextView tvAppVersion = (TextView) findViewById(R.id.tvAppVersion);
    tvAppVersion.setText(getString(R.string.lbl_app_version) + " " + BuildConfig.VERSION_NAME);

    sbUserMapID.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        tvMsg.setText("Progress: " + progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        tvMsg.setText("Selected: " + seekBar.getProgress());
      }
    });

    sbUserMapID.setVisibility(View.INVISIBLE);

    tvAppVersion.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {

        if (sbUserMapID.getVisibility() == View.INVISIBLE) {
          sbUserMapID.setVisibility(View.VISIBLE);
        } else {
          sbUserMapID.setVisibility(View.INVISIBLE);
          SharedPreferences mInSecurePrefs = getSharedPreferences(SECRET_PREF_NAME,
            MODE_PRIVATE);
          SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
          prefEdit.putString(PREF_KEY_UserMapID, "" + sbUserMapID.getProgress());
          prefEdit.apply();
          mUser.UserMapID = "" + sbUserMapID.getProgress();
        }

        Toast.makeText(getApplicationContext(),
          getString(R.string.lbl_app_version) + " " + BuildConfig.VERSION_NAME,
          Toast.LENGTH_LONG).show();

        return false;
      }
    });
    //TODO: Not to be set title here while using fragment.
    mTitle = getString(R.string.title_Home);
    setTitle(mTitle);
  }

  @Override
  public void onNavigationDrawerItemSelected(int MenuIndex, boolean fromSavedState) {
    // TODO: update the main content by replacing fragments
    SharedPreferences mInSecurePrefs = getSharedPreferences(SECRET_PREF_NAME,
      MODE_PRIVATE);
    //TODO: Proper Menu decoding to be done here so that position could be avoided
    final int ExitMenu = 4;
    Log.d(TAG, "Drawer MenuIndex:" + MenuIndex);
    if (mInSecurePrefs == null) {
      //Log.d("StartLogin: ", "Preference not found");
    } else {
      String MobileNo = mInSecurePrefs.getString(PREF_KEY_MOBILE, null);
      if ((MobileNo == null) && (MenuIndex < ExitMenu)) {
        startActivityForResult(new Intent(getApplicationContext(),
          LoginActivity.class), UPDATE_PROFILE_REQUEST);
      } else if (!fromSavedState) {

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (MenuIndex) {
          //case 1:
            /*fragmentManager.beginTransaction()
              .replace(
                R.id.fragmentHolder,
                PlaceholderFragment.newInstance(MenuIndex + 1)
              )
              .commit();
            setTitle(getString(R.string.btn_update_profile_text));*/
            //break;

          case 1:
            /*try {
              fragmentManager.beginTransaction()
                .replace(
                  R.id.fragmentHolder,
                  MprFragment.newInstance(mUser.UserMapID, "")
                ).commit();
            } catch (Exception e) {
              Log.d(TAG, "Error in Fragment");
            }
            setTitle(getString(R.string.title_activity_scheme));*/

            startActivity(new Intent(getApplicationContext(), SchemeActivity.class)
              .putExtra(SchemeActivity.UID, mUser.MobileNo));
            break;

          case 2:
            startActivity(new Intent(getApplicationContext(), GroupSMS.class)
            .putExtra(GroupSMS.LAUNCH_KEY,DashAIO.SECRET_PREF_NAME));
            break;

          case 3:
            startActivity(new Intent(getApplicationContext(), GroupActivity.class)
            .putExtra(GroupActivity.LAUNCH_KEY,DashAIO.SECRET_PREF_NAME));
            break;

          case ExitMenu:
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            break;
        }
      }
    }
  }

  @Override
  public void setTitle(CharSequence title) {
    mTitle = title;
    getSupportActionBar().setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (!mNavigationDrawerFragment.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      getMenuInflater().inflate(R.menu.dash_aio, menu);
      restoreActionBar();
      return true;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    switch (id) {
      case R.id.sync_otp:
        SyncOTP();
        return true;

      case R.id.register_again:
        SharedPreferences mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME,
          MODE_PRIVATE);
        mAccountDb.update("**********", "****************", mUser.getMobileNo(), AccountDb.OtpType.TOTP, 0);
        mAccountDb.deleteAllData();
        mAccountDb.close();
        SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
        prefEdit.clear();
        prefEdit.apply();
        Toast.makeText(getApplicationContext(),
          getString(R.string.register_again_msg),
          Toast.LENGTH_LONG).show();
        finish();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void onSectionAttached(int number) {
    switch (number) {
      case 1:
        mTitle = getString(R.string.title_Home);
        break;
      case 2:
        //mTitle = getString(R.string.title_activity_scheme);
        break;
      case 3:
        //mTitle = getString(R.string.title_activity_group_sms);
        break;
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  public void hideViewsForFragment() {
    tvMsg.setVisibility(View.GONE);
    sbUserMapID.setVisibility(View.GONE);
  }

  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {

    if (requestCode == UPDATE_PROFILE_REQUEST) {
      if (resultCode == RESULT_OK) {
        SharedPreferences mInSecurePrefs = getSharedPreferences(SECRET_PREF_NAME,
          MODE_PRIVATE);
        SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
        //prefEdit.putString(PREF_KEY_UserMapID, data.getStringExtra(PREF_KEY_UserMapID));
        prefEdit.putString(PREF_KEY_MOBILE, data.getStringExtra(PREF_KEY_MOBILE));
        prefEdit.putString(PREF_KEY_NAME, data.getStringExtra(PREF_KEY_NAME));
        prefEdit.putString(PREF_KEY_EMAIL, data.getStringExtra(PREF_KEY_EMAIL));
        prefEdit.putString(PREF_KEY_POST, data.getStringExtra(PREF_KEY_POST));
        prefEdit.apply();
        //Log.d("onActivityResult", "GroupSMS-RequestCode: " + requestCode + ":"
        //  + resultCode + " UserMapID:" + data.getStringExtra(PREF_KEY_UserMapID)
        //  + " =>" + mInSecurePrefs.getAll().toString());
        startActivity(new Intent(getApplicationContext(), DashAIO.class));
        finish();
      } else {
        Toast.makeText(getApplicationContext(),
          "Unable to update profile.",
          Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mAccountDb.close();
    rQueue.cancelAll(TAG);
  }

  private void SyncOTP() {
    final JSONObject jsonPost = new JSONObject();
    String OTP1;
    String OTP2;

    try {
      OTP1 = mOtpProvider.getNextCode(mUser.MobileNo);
      OTP2 = mOtpProvider.getNextCode(mUser.MobileNo);
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      tvMsg.setText("Error: " + e.getMessage()
        + " Mobile:" + mUser.MobileNo);
      return;
    }

    try {
      jsonPost.put("API", "SP");
      //jsonPost.put("OTP1", OTP1);
      //jsonPost.put("OTP2", OTP2);
      jsonPost.put("MDN", mUser.MobileNo);
      jsonPost.put("OTP", mUser.pin);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(
      Request.Method.POST,
      DashAIO.API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          //Log.d(TAG, "Sync: " + response.toString());
          Toast.makeText(getApplicationContext(),
            response.optString(DashAIO.KEY_STATUS),
            Toast.LENGTH_LONG).show();
          try {
            JSONObject respJson = response.getJSONArray("DB").getJSONObject(0);
            SharedPreferences mInSecurePrefs =
              getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
            prefEdit.putString(PREF_KEY_MOBILE, mUser.MobileNo);
            prefEdit.putString(PREF_KEY_NAME, respJson.optString("DisplayName"));
            prefEdit.putString(PREF_KEY_EMAIL, respJson.optString("eMailID"));
            prefEdit.putString(PREF_KEY_POST, respJson.optString("Designation"));
            prefEdit.putString(PREF_KEY_UserMapID, respJson.optString("UserMapID"));
            prefEdit.apply();
          } catch (JSONException e) {
            e.printStackTrace();
            tvMsg.setText("Error: " + e.getMessage());
          }

        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        tvMsg.setText("Error: " + error.getMessage());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    Toast.makeText(getApplicationContext(),
      getString(R.string.msg_sync_otp),
      Toast.LENGTH_LONG).show();
    //Log.d(TAG, jsonPost.toString() + mAccountDb.getSecret(mUser.MobileNo)
    //  + " " + mAccountDb.getCounter(mUser.MobileNo));
  }

  private void showAttendance() {
    final JSONObject jsonPost = new JSONObject();
    final String AR_API = API_HOST + "/apps/atnd-reg/android/api.php";

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      tvMsg.setText("You are not registered!");
      return;
    }

    try {
      jsonPost.put("API", "AR");
      jsonPost.put("MDN", mUser.MobileNo);
      jsonPost.put("OTP", mUser.pin);
    } catch (JSONException e) {
      e.printStackTrace();
      tvMsg.setText("Error: " + e.getMessage()
        + " Mobile:" + mUser.MobileNo);
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      AR_API, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          try {
            if (!response.optString(DashAIO.KEY_STATUS).equals("NA")) {
              JSONArray respJson = response.getJSONArray("DB");
              tvMsg.setText(respJson.toString());
            }
          } catch (JSONException e) {
            e.printStackTrace();
            tvMsg.setText("Error: " + e.getMessage());
          }

        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        tvMsg.setText("Error: " + error.getMessage());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    //Log.d(TAG, jsonPost.toString() + mAccountDb.getSecret(mUser.MobileNo)
    //  + " " + mAccountDb.getCounter(mUser.MobileNo));
  }

//  @Override
//  public void onFragmentInteraction(Long SchemeID, String SchemeName) {
//    Intent iWorks = new Intent(getApplicationContext(), UserActivity.class);
//    iWorks.putExtra(MprFragment.SID, SchemeID);
//    iWorks.putExtra(MprFragment.SN, SchemeName);
//    iWorks.putExtra(MprFragment.UID, mUser.UserMapID);
//    startActivity(iWorks);
//  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public PlaceholderFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
      PlaceholderFragment fragment = new PlaceholderFragment();
      Bundle args = new Bundle();
      args.putInt(ARG_SECTION_NUMBER, sectionNumber);
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      return inflater.inflate(R.layout.fragment_dash_aio, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      ((DashAIO) activity).onSectionAttached(
        getArguments().getInt(ARG_SECTION_NUMBER));
    }
  }
}
