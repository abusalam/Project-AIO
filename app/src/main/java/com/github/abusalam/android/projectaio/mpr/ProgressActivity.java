package com.github.abusalam.android.projectaio.mpr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class ProgressActivity extends ActionBarActivity {
    public static final String TAG = ProgressActivity.class.getSimpleName();

    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
     * generated for an account until the moment the next code can be generated for the account.
     * This is to prevent the user from generating too many HOTP codes in a short period of time.
     */
    private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;

    public static final String DYN_TITLE ="WPT";
    protected User mUser;
    private RequestQueue rQueue;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;

    private TextView tvPrgVal;
    private SeekBar sbProgress;
    private EditText etExpAmount;
    private EditText etRemarks;
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
        mUser.UserMapID = mInSecurePrefs.getLong(DashAIO.PREF_KEY_UserMapID, 0);
        mUser.MobileNo=mInSecurePrefs.getString(DashAIO.PREF_KEY_MOBILE, "");
        rQueue = VolleyAPI.getInstance(this).getRequestQueue();

        TextView tvWork = (TextView) findViewById(R.id.tvWork);
        TextView tvWorkBal = (TextView) findViewById(R.id.tvLblBalance);
        tvPrgVal = (TextView) findViewById(R.id.tvPrgVal);
        sbProgress = (SeekBar) findViewById(R.id.sbProgress);
        etExpAmount = (EditText) findViewById(R.id.etExpAmount);
        etRemarks = (EditText) findViewById(R.id.etRemarks);
        Button btnSave = (Button) findViewById(R.id.btnSave);

        mWork = getIntent().getExtras().getParcelable(WorkActivity.WorkName);


        tvWork.setText(mWork.getWorkName());
        tvWorkBal.setText(getString(R.string.lbl_balance) + mWork.getBalance());
        tvPrgVal.setText(": (" + mWork.getProgress() + "%)");
        sbProgress.setProgress(mWork.getProgress());
        sbProgress.setOnSeekBarChangeListener(new sbPrgListener());

        setTitle(getIntent().getExtras().getString(DYN_TITLE)
                + " : " + getString(R.string.title_activity_progress_mpr) + " (" + mWork.getProgress() + "%)");

        btnSave.setOnClickListener(new UpdateClickListener());
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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

    private class sbPrgListener implements SeekBar.OnSeekBarChangeListener{

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

            if (txtMsg.length() > 0) {
                Toast.makeText(getApplicationContext(), "Updating ... ", Toast.LENGTH_SHORT).show();
                try {
                    String oldPin = mUser.pin;
                    mUser.pin = mOtpProvider.getNextCode(mUser.MobileNo);
                    if (mUser.pin.equals(oldPin) || !mUser.hotpCodeGenerationAllowed) {
                        Toast.makeText(getApplicationContext(), "Please wait for a while to generate new OTP for"
                                + " MDN:" + mUser.MobileNo, Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        setProgress();
                    }
                } catch (OtpSourceException e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
                            + " MDN:" + mUser.MobileNo, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Temporarily disable code generation for this account
                mUser.hotpCodeGenerationAllowed = false;

                // The delayed operation below will be invoked once code generation is yet again allowed for
                // this account. The delay is in wall clock time (monotonically increasing) and is thus not
                // susceptible to system time jumps.
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mUser.hotpCodeGenerationAllowed = true;
                            }
                        },
                        HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
                );

                //UpdateProgress();

            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_warn_remarks), Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void setProgress() {

        final JSONObject jsonPost = new JSONObject();

        Log.e("P-Counter: ", "" + mAccountDb.getCounter(mUser.MobileNo));

        try {
            jsonPost.put("API", "UP");
            jsonPost.put("WID", mWork.getWorkID());
            jsonPost.put("EA", etExpAmount.getText());
            jsonPost.put("P", sbProgress.getProgress());
            jsonPost.put("B", mWork.getBalance());
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
                        Log.e(TAG, "Update Progress: " + response.toString());
                        Toast.makeText(getApplicationContext(),
                                response.optString(DashAIO.KEY_STATUS),
                                Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Log.e(TAG, jsonPost.toString());
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

        // Adding request to request queue
        jsonObjReq.setTag(TAG);
        rQueue.add(jsonObjReq);
        //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
    }
}
