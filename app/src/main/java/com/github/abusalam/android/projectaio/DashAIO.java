package com.github.abusalam.android.projectaio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.TextView;
import android.widget.Toast;

import com.github.abusalam.android.projectaio.ajax.NetConnection;


public class DashAIO extends ActionBarActivity
    implements NavigationDrawerFragment.NavigationDrawerCallbacks {

  static final int UPDATE_PROFILE_REQUEST = 0;
  static final String KEY_SENT_ON = "ST";
  static final String KEY_STATUS = "MSG";
  static final String KEY_API = "API";
  static final String API_URL = "http://www.paschimmedinipur.gov.in/apps/android/api.php";
  static final String SECRET_PREF_NAME = "mPrefSecrets";

  // WebServer Request URL
  //String serverURL = "http://echo.jsontest.com/key/value/one/two";
  //String serverURL = "http://10.42.0.1/apps/android/api.php";
  //String serverURL = "http://www.paschimmedinipur.gov.in/apps/android/api.php";
  static final String PREF_KEY_UserID = "mUserID";
  static final String PREF_KEY_NAME = "pref_display_name";
  static final String PREF_KEY_POST = "pref_designation";
  static final String PREF_KEY_EMAIL = "pref_email";
  static final String PREF_KEY_MOBILE = "pref_mobile";

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

    mNavigationDrawerFragment = (NavigationDrawerFragment)
        getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    mTitle = getTitle();

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(
        R.id.navigation_drawer,
        (DrawerLayout) findViewById(R.id.drawer_layout));

    NetConnection IC = new NetConnection(getApplicationContext());
    TextView tvNetConn = (TextView) findViewById(R.id.tvNetConn);
    TextView tvUserName = (TextView) findViewById(R.id.tvUserName);
    TextView tvDesg = (TextView) findViewById(R.id.tvDesignation);
    TextView tvEMail = (TextView) findViewById(R.id.tvEMailID);
    TextView tvMobile = (TextView) findViewById(R.id.tvMobileNo);

    if (IC.isDeviceConnected()) {
      tvNetConn.setText(getString(R.string.IC));
      SharedPreferences settings = getSharedPreferences(SECRET_PREF_NAME,MODE_PRIVATE);
      tvUserName.setText(settings.getString(PREF_KEY_NAME, ""));
      tvDesg.setText(settings.getString(PREF_KEY_POST, ""));
      tvEMail.setText(settings.getString(PREF_KEY_EMAIL, ""));
      tvMobile.setText(settings.getString(PREF_KEY_MOBILE, ""));
    } else {
      tvNetConn.setText(getString(R.string.NC));
    }

  }

  @Override
  public void onNavigationDrawerItemSelected(int position) {
    // update the main content by replacing fragments
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction()
        .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
        .commit();
  }

  public void onSectionAttached(int number) {
    String[] mDrawerMenuList = getResources().getStringArray(R.array.drawer_menu_list);

    if (number == mDrawerMenuList.length) {
      number = 0;
    } else {
      //mTitle = mDrawerMenuList[number - 1];
    }
    switch (number) {
      case 1:
        break;
      case 2:
        SharedPreferences mInSecurePrefs = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);
        if (mInSecurePrefs == null) {
          Log.e("StartLogin: ", "Preference not found");
        } else {
          String mUserID = mInSecurePrefs.getString(PREF_KEY_UserID, null);
          if (mUserID == null) {
            startActivityForResult(new Intent(getApplicationContext(), LoginActivity.class), UPDATE_PROFILE_REQUEST);
          } else {
            startActivity(new Intent(getApplicationContext(), GroupSMS.class));
          }
        }
        break;
      case 3:
      case 4:
        break;
      case 6:
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        break;
      case 0:
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        break;
    }
  }

  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {

    if (requestCode == UPDATE_PROFILE_REQUEST) {
      if (resultCode == RESULT_OK) {
        String mUserID = data.getStringExtra(PREF_KEY_UserID);
        SharedPreferences mInSecurePrefs = getSharedPreferences(SECRET_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
        prefEdit.putString(PREF_KEY_UserID, mUserID);
        prefEdit.putString(PREF_KEY_MOBILE,data.getStringExtra(PREF_KEY_UserID));
        prefEdit.putString(PREF_KEY_NAME,data.getStringExtra(PREF_KEY_NAME));
        prefEdit.putString(PREF_KEY_EMAIL,data.getStringExtra(PREF_KEY_EMAIL));
        prefEdit.putString(PREF_KEY_POST,data.getStringExtra(PREF_KEY_POST));
        prefEdit.apply();
        Log.e("onActivityResult-GroupSMS", "RequestCode: " + requestCode
            + ":" + resultCode + mUserID + " =>" + mInSecurePrefs.getAll().toString());
        startActivity(new Intent(getApplicationContext(), GroupSMS.class));
      }else{
        Toast.makeText(getApplicationContext(),"Unable to update profile.",Toast.LENGTH_LONG).show();
      }
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
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
    if (id == R.id.action_settings) {
      startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

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
      View rootView = inflater.inflate(R.layout.fragment_dash_aio, container, false);
      return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      ((DashAIO) activity).onSectionAttached(
          getArguments().getInt(ARG_SECTION_NUMBER));
    }
  }

}
