package com.github.abusalam.android.projectaio;

import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ProgressMPR extends ActionBarActivity {
    public static final String TAG = ProgressMPR.class.getSimpleName();
    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
     * generated for an account until the moment the next code can be generated for the account.
     * This is to prevent the user from generating too many HOTP codes in a short period of time.
     */
    private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;
    /**
     * The maximum amount of time (milliseconds) for which a HOTP code is displayed after it's been
     * generated.
     */
    private static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;
    protected PinInfo mUser;
    private JSONArray respJsonArray;
    private RequestQueue rQueue;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;

    private Spinner spnSchemes;
    private Spinner spnWorks;
    private EditText etExpAmount;
    private EditText etRemarks;

    private int mSelectedItemIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_mpr);
        SharedPreferences mInSecurePrefs;
        mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

        mAccountDb = new AccountDb(this);
        mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

        mUser = new PinInfo();
        mUser.user = mInSecurePrefs.getString(DashAIO.PREF_KEY_UserID, null);
        rQueue = VolleyAPI.getInstance(this).getRequestQueue();

        spnSchemes = (Spinner) findViewById(R.id.spnSchemes);
        spnWorks = (Spinner) findViewById(R.id.spnWorks);
        etExpAmount = (EditText) findViewById(R.id.etExpAmount);
        etRemarks = (EditText) findViewById(R.id.etRemarks);

        getUserSchemes();
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

    /**
     * A tuple of user, OTP value, and type, that represents a particular user.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private static class PinInfo {
        private String pin; // calculated OTP, or a placeholder if not calculated
        private String user;
        private boolean isHotp = true; // used to see if button needs to be displayed

        /**
         * HOTP only: Whether code generation is allowed for this account.
         */
        private boolean hotpCodeGenerationAllowed = true;
    }


    private void getUserSchemes() {

        final JSONObject jsonPost = new JSONObject();

        try {
            mUser.pin = mOtpProvider.getNextCode(mUser.user);
            mUser.hotpCodeGenerationAllowed = false;
        } catch (OtpSourceException e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
                    + " MDN:" + mUser.user, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            jsonPost.put("API", "US");
            jsonPost.put("MDN", mUser.user);
            jsonPost.put("UID", "1"); // TODO Supply Dynamic UserMapID instead of Static
            jsonPost.put("OTP", mUser.pin);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                DashAIO.API_URL, jsonPost,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "UserSchemes: " + response.toString());
                        Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS), Toast.LENGTH_SHORT).show();
                        try {
                            respJsonArray = response.getJSONArray("DB");
                            ArrayList<String> groupList = new ArrayList<String>();

                            for (int i = 0; i < respJsonArray.length(); i++) {
                                groupList.add(respJsonArray.getJSONObject(i).optString("GroupName"));
                            }
                            // Spinner adapter
                            spnSchemes.setAdapter(new ArrayAdapter<String>(ProgressMPR.this,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    groupList));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }

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
        Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_LONG).show();
    }
}
