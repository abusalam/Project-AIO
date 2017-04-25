package com.github.abusalam.android.projectaio.sms;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.abusalam.android.projectaio.DashAIO;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.AccountDb;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpProvider;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.TotpClock;
import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.User;

public class GroupActivity extends ActionBarActivity implements GroupFragment.OnListFragmentInteractionListener {
  private User mUser;
  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_groups);

    SharedPreferences mInSecurePrefs;
    mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

    mAccountDb = new AccountDb(this);
    mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

    mUser = new User();
    mUser.setMobileNo(mInSecurePrefs.getString(DashAIO.PREF_KEY_MOBILE, null));
    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "OTP Error: " + e.getMessage()
        + " Mobile:" + mUser.MobileNo, Toast.LENGTH_LONG).show();
    }
    FragmentManager fragmentManager = getSupportFragmentManager();

    fragmentManager.beginTransaction()
      .setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_top, R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom)
      .replace(
        R.id.fragmentHolder,
        GroupFragment.newInstance(mUser.getMobileNo(), mUser.getPin())
      ).commit();
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public void onListFragmentInteraction(Group item) {
    Log.d("SelectedGroup: ", item.getGroupName());

    ContactFragment contactFragment = ContactFragment.newInstance(mUser.getMobileNo(), mUser.getPin(), item.getGroupName());
    Bundle args = new Bundle();
    args.putString("MDN", mUser.getMobileNo());
    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "OTP Error: " + e.getMessage()
        + " Mobile:" + mUser.MobileNo, Toast.LENGTH_LONG).show();
    }
    args.putString("OTP", mUser.getPin());
    args.putString("GRP", item.getGroupName());
    contactFragment.setArguments(args);

    FragmentManager fragmentManager = getSupportFragmentManager();

    fragmentManager.beginTransaction()
      .replace(R.id.fragmentHolder, contactFragment)
      .addToBackStack(null)
      .commit();
    setTitle(item.getGroupName());
  }
}
