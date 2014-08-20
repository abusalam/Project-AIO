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
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends ActionBarActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final long VIBRATE_DURATION = 200L;
    private static final int MIN_KEY_BYTES = 10;

    protected EditText etMobileNo;
    protected EditText mKeyEntryField;
    protected ImageButton GetImgButton;
    protected TextView msgLoginText;
    protected ProgressBar pbLoginWait;
    protected Button btnLogin;
    protected Button btnEnterKey;
    protected TextView tvOTP;
    protected Button btnNextCode;

    protected RequestQueue rQueue;
    protected JSONObject apiRespUserStat;




    View.OnClickListener btnUpdateClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            etMobileNo.setVisibility(View.GONE);
            GetImgButton.setVisibility(View.GONE);
            msgLoginText.setText(getText(R.string.login_wait_message));
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
                            btnLogin.setVisibility(View.VISIBLE);
                            pbLoginWait.setVisibility(View.GONE);
                            msgLoginText.setText(response.optString(DashAIO.KEY_STATUS));
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
    };

    View.OnClickListener loginClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent data = new Intent();
            data.putExtra(DashAIO.PREF_KEY_UserID, apiRespUserStat.optString(DashAIO.PREF_KEY_UserID));
            data.putExtra(DashAIO.PREF_KEY_Secret, apiRespUserStat.optString(DashAIO.PREF_KEY_Secret));
            setResult(RESULT_OK, data);
            finish();
        }
    };

    View.OnClickListener enterKeyClick = new View.OnClickListener() {
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
    };

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
    private PinInfo[] mUsers = {};

    /**
     * A tuple of user, OTP value, and type, that represents a particular user.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private static class PinInfo {
        private String pin; // calculated OTP, or a placeholder if not calculated
        private String user;
        private boolean isHotp = false; // used to see if button needs to be displayed

        /** HOTP only: Whether code generation is allowed for this account. */
        private boolean hotpCodeGenerationAllowed;
    }

    /**
     * Listener for the Button that generates the next OTP value.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private class NextOtpButtonListener implements View.OnClickListener {
        private final Handler mHandler = new Handler();
        private final PinInfo mAccount;

        private NextOtpButtonListener(PinInfo account) {
            mAccount = account;
        }

        @Override
        public void onClick(View v) {
            int position = 0; // = findAccountPositionInList();
            if (position == -1) {
                throw new RuntimeException("Account not in list: " + mAccount);
            }

            try {
                computeAndDisplayPin(mAccount.user, position, true);
            } catch (OtpSourceException e) {
                //DependencyInjector.getOptionalFeatures().onAuthenticatorActivityGetNextOtpFailed(
                //        AuthenticatorActivity.this, mAccount.user, e);
                return;
            }

            final String pin = mAccount.pin;

            // Temporarily disable code generation for this account
            mAccount.hotpCodeGenerationAllowed = false;
            tvOTP.setText(pin); //mUserAdapter.notifyDataSetChanged();
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

        /**
         * Gets the position in the account list of the account this listener is associated with.
         *
         * @return {@code 0}-based position or {@code -1} if the account is not in the list.
         */
        private int findAccountPositionInList() {
            for (int i = 0, len = mUsers.length; i < len; i++) {
                if (mUsers[i] == mAccount) {
                    return i;
                }
            }

            return -1;
        }
    }

    /**
     * Computes the PIN and saves it in mUsers. This currently runs in the UI
     * thread so it should not take more than a second or so. If necessary, we can
     * move the computation to a background thread.
     *
     * @param user the user email to display with the PIN
     * @param position the index for the screen of this user and PIN
     * @param computeHotp true if we should increment counter and display new hotp
     */
    public void computeAndDisplayPin(String user, int position,
                                     boolean computeHotp) throws OtpSourceException {

        PinInfo currentPin;
        if (mUsers[position] != null) {
            currentPin = mUsers[position]; // existing PinInfo, so we'll update it
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

        mUsers[position] = currentPin;

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
                mKeyEntryField.setError(submitting ? getString(R.string.enter_key_too_short) : null);
                return false;
            } else {
                mKeyEntryField.setError(null);
                return true;
            }
        } catch (Base32String.DecodingException e) {
            mKeyEntryField.setError(getString(R.string.enter_key_illegal_char));
            return false;
        }
    }

    /**
     * Return key entered by user, replacing visually similar characters 1 and 0.
     */
    private String getEnteredKey() {
        String enteredKey = mKeyEntryField.getText().toString();
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

        mAccountDb=new AccountDb(this);


        msgLoginText = (TextView) findViewById(R.id.tvLoginMessage);
        etMobileNo = (EditText) findViewById(R.id.etUserMobile);
        mKeyEntryField = (EditText) findViewById(R.id.etSecretKey);
        pbLoginWait = (ProgressBar) findViewById(R.id.pbLoginWait);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnEnterKey = (Button) findViewById(R.id.btnEnterKey);
        btnNextCode=(Button) findViewById(R.id.btnScanBarcode);
        tvOTP=(TextView) findViewById(R.id.tvOTP);
        mUsers[0] = new PinInfo();
        mUsers[0].user="8972096989";


        GetImgButton = (ImageButton) findViewById(R.id.btnUpdateProfile);

        pbLoginWait.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);

        GetImgButton.setOnClickListener(btnUpdateClick);
        btnLogin.setOnClickListener(loginClick);
        btnEnterKey.setOnClickListener(enterKeyClick);
        btnNextCode.setOnClickListener(new NextOtpButtonListener(mUsers[0]));

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
