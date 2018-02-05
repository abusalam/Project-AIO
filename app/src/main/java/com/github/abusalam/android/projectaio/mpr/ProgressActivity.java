package com.github.abusalam.android.projectaio.mpr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
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
import com.github.abusalam.android.projectaio.User;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONException;
import org.json.JSONObject;


public class ProgressActivity extends ActionBarActivity {
  public static final String TAG = ProgressActivity.class.getSimpleName();
  public static final String DYN_TITLE = "WPT";
  public static final String LAUNCH_KEY = "ProgressKey";
  /**
   * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
   * generated for an account until the moment the next code can be generated for the account.
   * This is to prevent the user from generating too many HOTP codes in a short period of time.
   */
  private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;
  protected User mUser;
  private RequestQueue rQueue;
  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;

  private TextView tvPrgVal;
  private SeekBar sbProgress;
  private EditText etExpAmount;
  private EditText etRemarks;
  private Button btnSave;
  private Work mWork;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_progress_mpr);
    SharedPreferences mInSecurePrefs;
    mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

    mAccountDb = new AccountDb(this);
    mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

    mUser = new User();
    mUser.setUserMapID(mInSecurePrefs.getString(DashAIO.PREF_KEY_UserMapID, "Not Available"));
    mUser.setMobileNo( mInSecurePrefs.getString(DashAIO.PREF_KEY_MOBILE, ""));
    rQueue = VolleyAPI.getInstance(this).getRequestQueue();

    TextView tvWork = (TextView) findViewById(R.id.tvWork);
    TextView tvWorkBal = (TextView) findViewById(R.id.tvLblBalance);
    tvPrgVal = (TextView) findViewById(R.id.tvPrgVal);
    sbProgress = (SeekBar) findViewById(R.id.sbProgress);
    etExpAmount = (EditText) findViewById(R.id.etExpAmount);
    TextView tvExpAmount = (TextView) findViewById(R.id.tvExpAmount);
    TextView tvWorkRemark = (TextView) findViewById(R.id.tvWorkRemark);
    TextView tvRemark = (TextView) findViewById(R.id.tvRemarks);
    etRemarks = (EditText) findViewById(R.id.etRemarks);
    btnSave = (Button) findViewById(R.id.btnSave);

    mWork = getIntent().getExtras().getParcelable(WorkActivity.WorkName);


    tvWork.setText(mWork.getWorkName());
    tvWorkBal.setText(getString(R.string.lbl_balance) + " " + mWork.getBalance());
    tvPrgVal.setText(": (" + mWork.getProgress() + "%)");
    sbProgress.setProgress(mWork.getProgress());
    sbProgress.setOnSeekBarChangeListener(new sbPrgListener());
    tvWorkRemark.setText(mWork.getWorkRemarks());
    tvRemark.setText(getString(R.string.lblRemarks) + " " + mWork.getRemarks());

    setTitle(getIntent().getExtras().getString(DYN_TITLE)
      + " : " + getString(R.string.title_activity_progress_mpr)
      + " (" + mWork.getProgress() + "%)");

    if (mWork.isEditable()) {
      btnSave.setOnClickListener(new UpdateClickListener());
    } else {
      btnSave.setVisibility(View.GONE);
      etExpAmount.setVisibility(View.GONE);
      etRemarks.setVisibility(View.GONE);
      tvExpAmount.setVisibility(View.GONE);
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_progress_mpr, menu);
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
  protected void onDestroy() {
    super.onDestroy();
    mAccountDb.close();
    rQueue.cancelAll(TAG);
  }

  private void setProgress() {
    final JSONObject jsonPost = new JSONObject();

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
          + " MDN:" + mUser.MobileNo, Toast.LENGTH_SHORT).show();
      return;
    }

    try {
      jsonPost.put("API", "UP");
      jsonPost.put("MDN", mUser.getMobileNo());
      jsonPost.put("OTP", mUser.getPin());
      jsonPost.put("WID", mWork.getWorkID());
      jsonPost.put("EA", etExpAmount.getText());
      jsonPost.put("P", sbProgress.getProgress());
      jsonPost.put("R", etRemarks.getText());
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      SchemeActivity.API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          //Log.d(TAG, "Update Progress: " + response.toString());
          Toast.makeText(getApplicationContext(),
            response.optString(DashAIO.KEY_STATUS),
            Toast.LENGTH_SHORT).show();
          if (response.optBoolean(DashAIO.KEY_API)) {
            btnSave.setVisibility(View.GONE);
          }

        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        //Log.d(TAG, jsonPost.toString());
      }
    }
    );

    Handler mHandler = new Handler();
    mHandler.postDelayed(
      new Runnable() {
        @Override
        public void run() {
          mUser.hotpCodeGenerationAllowed = true;
        }
      },
      HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
    );

    jsonObjReq.setTag(TAG);
    jsonObjReq.setShouldCache(false);
    rQueue.add(jsonObjReq);
  }

  private class sbPrgListener implements SeekBar.OnSeekBarChangeListener {

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
      tvPrgVal.setText(": (" + i + "%)");
      mWork.setProgress(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      seekBar.setProgress(mWork.getProgress());
    }
  }

  private class UpdateClickListener implements View.OnClickListener {
    private final Handler mHandler = new Handler();

    @Override
    public void onClick(View view) {

      String txtMsg = etRemarks.getText().toString();
      Long etExpAmt = Long.parseLong(etExpAmount.getText().toString());
      if (etExpAmt > mWork.getBalance()) {
        Toast.makeText(getApplicationContext(),
          getString(R.string.msg_insufficient_balance),
          Toast.LENGTH_SHORT).show();
        return;
      }

      if (txtMsg.length() > 0) {
        Toast.makeText(getApplicationContext(),
          "Updating ... ", Toast.LENGTH_SHORT).show();
        try {
          String oldPin = mUser.getPin();
          mUser.setPin(mOtpProvider.getNextCode(mUser.getMobileNo()));
          if (mUser.getPin().equals(oldPin) || !mUser.hotpCodeGenerationAllowed) {
            Toast.makeText(getApplicationContext(),
              getString(R.string.msg_otp_delayed) + mUser.getMobileNo(),
              Toast.LENGTH_LONG).show();
            return;
          } else {
            setProgress();
          }
        } catch (OtpSourceException e) {
          Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
            + " MDN:" + mUser.getMobileNo(), Toast.LENGTH_SHORT).show();
          return;
        }

        // Temporarily disable code generation for this account
        mUser.hotpCodeGenerationAllowed = false;

        /**
         * The delayed operation below will be invoked once code
         * generation is yet again allowed for this account. The delay is in wall
         * clock time (monotonically increasing) and is thus not susceptible
         * to system time jumps.
         */

        mHandler.postDelayed(
          new Runnable() {
            @Override
            public void run() {
              mUser.hotpCodeGenerationAllowed = true;
            }
          },
          HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
        );

      } else {
        Toast.makeText(getApplicationContext(),
          getString(R.string.msg_warn_remarks),
          Toast.LENGTH_SHORT).show();
      }

    }
  }
}