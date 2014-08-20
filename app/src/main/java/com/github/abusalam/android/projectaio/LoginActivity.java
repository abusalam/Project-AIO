package com.github.abusalam.android.projectaio;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.AccountDb;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.Base32String;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpProvider;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.TotpClock;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends ActionBarActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final long VIBRATE_DURATION = 200L;
    private static final int MIN_KEY_BYTES = 10;

    protected EditText etMobileNo;
    protected EditText etSecretKey;
    protected ImageButton btnRegister;
    protected TextView tvLoginMessage;
    protected ProgressBar pbLoginWait;
    protected Button btnStartMessaging;
    protected Button btnSaveKey;
    protected TextView tvOTP;
    protected Button btnVerifyOTP;

    protected RequestQueue rQueue;
    protected JSONObject apiRespUserStat;


    /**
     * Listener for the Button that registers the user.
     */
    private class RegisterButtonListener implements OnClickListener {
        @Override
        public void onClick(View view) {

            etMobileNo.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            tvLoginMessage.setText(getText(R.string.login_wait_message));
            pbLoginWait.setVisibility(View.VISIBLE);


            JSONObject jsonPost = new JSONObject();

            try {
                jsonPost.put("API", "RU");
                jsonPost.put("MDN", etMobileNo.getText());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    DashAIO.API_URL, jsonPost,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, response.toString());
                            Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS), Toast.LENGTH_SHORT).show();
                            apiRespUserStat = response;

                            etSecretKey.setVisibility(View.VISIBLE);
                            btnSaveKey.setVisibility(View.VISIBLE);
                            tvOTP.setVisibility(View.VISIBLE);
                            btnVerifyOTP.setVisibility(View.VISIBLE);

                            pbLoginWait.setVisibility(View.GONE);
                            tvLoginMessage.setText(response.optString(DashAIO.KEY_STATUS));
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String msgError = "Error: " + error.getMessage();
                    Log.e(TAG, msgError);
                    Toast.makeText(getApplicationContext(), msgError, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            );

            // Adding request to request queue
            jsonObjReq.setTag(TAG);
            rQueue.add(jsonObjReq);
        }
    }

    /**
     * Listener for the Button that saves the user Credentials for future use
     * and completes the registration process.
     */
    private class StartMessagingButtonListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            Intent data = new Intent();
            data.putExtra(DashAIO.PREF_KEY_UserID, apiRespUserStat.optString(DashAIO.PREF_KEY_UserID));
            setResult(RESULT_OK, data);
            finish();
        }
    }

    /**
     * Listener for the Button that Validates and Saves the HOTP Secret Key of the user.
     */
    private class SaveKeyButtonListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            if (validateKeyAndUpdateStatus(true)) {
                saveSecret(LoginActivity.this,
                        etMobileNo.getText().toString(),
                        getEnteredKey(),
                        null,
                        AccountDb.OtpType.HOTP,
                        AccountDb.DEFAULT_HOTP_COUNTER);
            }
        }
    }

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

    /**
     * Phase of TOTP countdown indicators. The phase is in {@code [0, 1]} with {@code 1} meaning
     * full time step remaining until the code refreshes, and {@code 0} meaning the code is refreshing
     * right now.
     */
    private double mTotpCountdownPhase;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;
    private PinInfo mUser;

    /**
     * A tuple of user, OTP value, and type, that represents a particular user.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private static class PinInfo {
        private String pin; // calculated OTP, or a placeholder if not calculated
        private String user;
        private boolean isHotp = false; // used to see if button needs to be displayed

        /**
         * HOTP only: Whether code generation is allowed for this account.
         */
        private boolean hotpCodeGenerationAllowed;
    }

    /**
     * Listener for the Button that generates the next OTP value.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private class NextOtpButtonListener implements OnClickListener {
        private final Handler mHandler = new Handler();
        private final PinInfo mAccount;

        private NextOtpButtonListener(PinInfo account) {
            mAccount = account;
        }

        @Override
        public void onClick(View v) {

            try {
                computeAndDisplayPin(mAccount.user, /*position,*/ true);
            } catch (OtpSourceException e) {
                //DependencyInjector.getOptionalFeatures().onAuthenticatorActivityGetNextOtpFailed(
                //        AuthenticatorActivity.this, mAccount.user, e);
                return;
            }

            final String pin = mAccount.pin;

            // Temporarily disable code generation for this account
            mAccount.hotpCodeGenerationAllowed = false;
            tvOTP.setText(pin); //mUserAdapter.notifyDataSetChanged();

            // Verify the PIN with Server
            JSONObject jsonPost = new JSONObject();

            try {
                jsonPost.put("API", "OT");
                jsonPost.put("MDN", etMobileNo.getText());
                jsonPost.put("OTP", pin);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    DashAIO.API_URL, jsonPost,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, response.toString());
                            Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS)
                                    + " Counter: " + mAccountDb.getCounter(mAccount.user), Toast.LENGTH_SHORT).show();
                            apiRespUserStat = response;
                            if (response.optBoolean("API")) {
                                etSecretKey.setVisibility(View.GONE);
                                btnStartMessaging.setVisibility(View.VISIBLE);
                                btnSaveKey.setVisibility(View.GONE);
                                btnVerifyOTP.setVisibility(View.GONE);
                                tvOTP.setVisibility(View.GONE);
                            }
                            tvLoginMessage.setText(response.optString(DashAIO.KEY_STATUS));
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String msgError = "Error: " + error.getMessage();
                    Log.e(TAG, msgError);
                    Toast.makeText(getApplicationContext(), msgError, Toast.LENGTH_LONG).show();
                }
            }
            );

            // Adding request to request queue
            jsonObjReq.setTag(TAG);
            rQueue.add(jsonObjReq);


            // The delayed operation below will be invoked once code generation is yet again allowed for
            // this account. The delay is in wall clock time (monotonically increasing) and is thus not
            // susceptible to system time jumps.
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccount.hotpCodeGenerationAllowed = true;
                            //mUserAdapter.notifyDataSetChanged();
                        }
                    },
                    HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES);
            // The delayed operation below will hide this OTP to prevent the user from seeing this OTP
            // long after it's been generated (and thus hopefully used).
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!pin.equals(mAccount.pin)) {
                                return;
                            }
                            mAccount.pin = getString(R.string.empty_pin);
                            tvOTP.setText(mAccount.pin); //mUserAdapter.notifyDataSetChanged();
                        }
                    },
                    HOTP_DISPLAY_TIMEOUT);
        }
    }

    /**
     * Computes the PIN and saves it in mUsers. This currently runs in the UI
     * thread so it should not take more than a second or so. If necessary, we can
     * move the computation to a background thread.
     *
     * @param user        the user email to display with the PIN
     *                    //@param position the index for the screen of this user and PIN
     * @param computeHotp true if we should increment counter and display new hotp
     */
    public void computeAndDisplayPin(String user, /*int position,*/
                                     boolean computeHotp) throws OtpSourceException {

        PinInfo currentPin;
        if (mUser != null) {
            currentPin = mUser; // existing PinInfo, so we'll update it
        } else {
            currentPin = new PinInfo();
            currentPin.pin = getString(R.string.empty_pin);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        AccountDb.OtpType type = mAccountDb.getType(user);
        currentPin.isHotp = (type == AccountDb.OtpType.HOTP);

        currentPin.user = user;

        if (!currentPin.isHotp || computeHotp) {
            // Always safe to recompute, because this code path is only
            // reached if the account is:
            // - Time-based, in which case getNextCode() does not change state.
            // - Counter-based (HOTP) and computeHotp is true.
            currentPin.pin = mOtpProvider.getNextCode(user);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        mUser = currentPin;

    }


    /**
     * Verify that the input field contains a valid base32 string,
     * and meets minimum key requirements.
     */
    private boolean validateKeyAndUpdateStatus(boolean submitting) {
        String userEnteredKey = getEnteredKey();
        try {
            byte[] decoded = Base32String.decode(userEnteredKey);
            if (decoded.length < MIN_KEY_BYTES) {
                // If the user is trying to submit a key that's too short, then
                // display a message saying it's too short.
                etSecretKey.setError(submitting ? getString(R.string.enter_key_too_short) : null);
                return false;
            } else {
                etSecretKey.setError(null);
                return true;
            }
        } catch (Base32String.DecodingException e) {
            etSecretKey.setError(getString(R.string.enter_key_illegal_char));
            return false;
        }
    }

    /**
     * Return key entered by user, replacing visually similar characters 1 and 0.
     */
    private String getEnteredKey() {
        String enteredKey = etSecretKey.getText().toString();
        return enteredKey.replace('1', 'I').replace('0', 'O');
    }

    /**
     * Saves the secret key to local storage on the phone.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     * @return {@code true} if the secret was saved, {@code false} otherwise.
     */
    static boolean saveSecret(Context context, String user, String secret,
                              String originalUser, AccountDb.OtpType type, Integer counter) {
        if (originalUser == null) {  // new user account
            originalUser = user;
        }
        if (secret != null) {
            AccountDb accountDb = new AccountDb(context);
            accountDb.update(user, secret, originalUser, type, counter);
            //DependencyInjector.getOptionalFeatures().onAuthenticatorActivityAccountSaved(context, user);
            // TODO: Consider having a display message that activities can call and it
            //       will present a toast with a uniform duration, and perhaps update
            //       status messages (presuming we have a way to remove them after they
            //       are stale).
            Toast.makeText(context, R.string.secret_saved, Toast.LENGTH_LONG).show();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(VIBRATE_DURATION);
            return true;
        } else {
            Log.e(TAG, "Trying to save an empty secret key");
            Toast.makeText(context, R.string.error_empty_secret, Toast.LENGTH_LONG).show();
            return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAccountDb = new AccountDb(this);
        mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

        tvLoginMessage = (TextView) findViewById(R.id.tvLoginMessage);
        etMobileNo = (EditText) findViewById(R.id.etUserMobile);
        etSecretKey = (EditText) findViewById(R.id.etSecretKey);
        pbLoginWait = (ProgressBar) findViewById(R.id.pbLoginWait);
        btnStartMessaging = (Button) findViewById(R.id.btnStartMessaging);
        btnSaveKey = (Button) findViewById(R.id.btnSaveKey);
        btnVerifyOTP = (Button) findViewById(R.id.btnVerifyOTP);
        tvOTP = (TextView) findViewById(R.id.tvOTP);
        mUser = new PinInfo();
        mUser.user = "8972096989";


        btnRegister = (ImageButton) findViewById(R.id.btnUpdateProfile);

        pbLoginWait.setVisibility(View.GONE);
        btnStartMessaging.setVisibility(View.GONE);
        btnSaveKey.setVisibility(View.GONE);
        btnVerifyOTP.setVisibility(View.GONE);
        etSecretKey.setVisibility(View.GONE);
        tvOTP.setVisibility(View.GONE);

        btnRegister.setOnClickListener(new RegisterButtonListener());
        btnStartMessaging.setOnClickListener(new StartMessagingButtonListener());
        btnSaveKey.setOnClickListener(new SaveKeyButtonListener());
        btnVerifyOTP.setOnClickListener(new NextOtpButtonListener(mUser));

        rQueue = VolleyAPI.getInstance(this).getRequestQueue();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VolleyAPI.getInstance(LoginActivity.this).getRequestQueue().cancelAll(TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
