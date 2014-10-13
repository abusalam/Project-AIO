package com.github.abusalam.android.projectaio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
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
import com.github.abusalam.android.projectaio.GoogleAuthenticator.Utilities;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends ActionBarActivity {

  static final int SCAN_REQUEST = 31337;
  private static final String TAG = LoginActivity.class.getSimpleName();
  private static final String OTP_SCHEME = "otpauth";
  private static final String TOTP = "totp"; // time-based
  private static final String HOTP = "hotp"; // counter-based
  private static final String SECRET_PARAM = "secret";
  private static final String COUNTER_PARAM = "counter";

  private static final long VIBRATE_DURATION = 200L;
  private static final int MIN_KEY_BYTES = 10;
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
  protected EditText etMobileNo;
  protected EditText etSecretKey;
  protected ImageButton btnRegister;
  protected TextView tvLoginMessage;
  protected ProgressBar pbLoginWait;
  protected Button btnStartMessaging;
  protected Button btnSaveKey;
  protected TextView tvOTP;
  protected Button btnVerifyOTP;
  protected ImageButton btnScanOR;
  protected RequestQueue rQueue;
  protected JSONObject apiRespUserStat;
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
      accountDb.close();
      return true;
    } else {
      Log.e(TAG, "Trying to save an empty secret key");
      Toast.makeText(context, R.string.error_empty_secret, Toast.LENGTH_LONG).show();
      return false;
    }
  }

  private static String validateAndGetUserInPath(String path) {
    if (path == null || !path.startsWith("/")) {
      return null;
    }
    // path is "/user", so remove leading "/", and trailing white spaces
    String user = path.substring(1).trim();
    if (user.length() == 0) {
      return null; // only white spaces.
    }
    return user;
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

  private void scanBarcode() {
    Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
    intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
    intentScan.putExtra("SAVE_HISTORY", false);
    try {
      startActivityForResult(intentScan, SCAN_REQUEST);
    } catch (ActivityNotFoundException error) {
      Intent intent = new Intent(Intent.ACTION_VIEW,
          Uri.parse(Utilities.ZXING_MARKET));
      try {
        startActivity(intent);
      } catch (ActivityNotFoundException e) { // if no Market app
        intent = new Intent(Intent.ACTION_VIEW,
            Uri.parse(Utilities.ZXING_DIRECT));
        startActivity(intent);
      }
      Toast.makeText(this, R.string.install_dialog_message, Toast.LENGTH_LONG).show();
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
    btnScanOR = (ImageButton) findViewById(R.id.btnScanQR);
    btnScanOR.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        scanBarcode();
      }
    });


    btnRegister = (ImageButton) findViewById(R.id.btnUpdateProfile);

    pbLoginWait.setVisibility(View.GONE);
    btnStartMessaging.setVisibility(View.GONE);
    btnSaveKey.setVisibility(View.GONE);
    btnVerifyOTP.setVisibility(View.GONE);
    etSecretKey.setVisibility(View.GONE);
    tvOTP.setVisibility(View.GONE);
    btnScanOR.setVisibility(View.GONE);

    btnRegister.setOnClickListener(new RegisterButtonListener());
    btnStartMessaging.setOnClickListener(new StartMessagingButtonListener());
    btnSaveKey.setOnClickListener(new SaveKeyButtonListener());
    btnVerifyOTP.setOnClickListener(new NextOtpButtonListener(mUser));

    rQueue = VolleyAPI.getInstance(this).getRequestQueue();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Log.i(getString(R.string.app_name), TAG + ": onActivityResult");
    if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
      // Grab the scan results and convert it into a URI
      String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
      Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
      interpretScanResult(uri, false);
    }
  }

  /**
   * Interprets the QR code that was scanned by the user.  Decides whether to
   * launch the key provisioning sequence or the OTP seed setting sequence.
   *
   * @param scanResult        a URI holding the contents of the QR scan result
   * @param confirmBeforeSave a boolean to indicate if the user should be
   *                          prompted for confirmation before updating the otp
   *                          account information.
   */
  private void interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {
    // The scan result is expected to be a URL that adds an account.

    // Sanity check
    if (scanResult == null) {
      Toast.makeText(this, "Null QR Code", Toast.LENGTH_LONG).show();
      return;
    }

    // See if the URL is an account setup URL containing a shared secret
    if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
      parseSecret(scanResult, confirmBeforeSave);
    } else {
      Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Parses a secret value from a URI. The format will be:
   * <p/>
   * otpauth://totp/user@example.com?secret=FFF...
   * otpauth://hotp/user@example.com?secret=FFF...&counter=123
   *
   * @param uri               The URI containing the secret key
   * @param confirmBeforeSave a boolean to indicate if the user should be
   *                          prompted for confirmation before updating the otp
   *                          account information.
   */
  private void parseSecret(Uri uri, boolean confirmBeforeSave) {
    final String scheme = uri.getScheme().toLowerCase();
    final String path = uri.getPath();
    final String authority = uri.getAuthority();
    final String user;
    final String secret;
    final AccountDb.OtpType type;
    final Integer counter;

    if (!OTP_SCHEME.equals(scheme)) {
      Log.e(getString(R.string.app_name), TAG + ": Invalid or missing scheme in uri");
      Toast.makeText(this, "Invalid QR Code OTP Scheme", Toast.LENGTH_LONG).show();
      return;
    }

    if (TOTP.equals(authority)) {
      type = AccountDb.OtpType.TOTP;
      counter = AccountDb.DEFAULT_HOTP_COUNTER; // only interesting for HOTP
    } else if (HOTP.equals(authority)) {
      type = AccountDb.OtpType.HOTP;
      String counterParameter = uri.getQueryParameter(COUNTER_PARAM);
      if (counterParameter != null) {
        try {
          counter = Integer.parseInt(counterParameter);
        } catch (NumberFormatException e) {
          Log.e(getString(R.string.app_name), TAG + ": Invalid counter in uri");
          Toast.makeText(this, "Invalid QR Code Counter", Toast.LENGTH_LONG).show();
          return;
        }
      } else {
        counter = AccountDb.DEFAULT_HOTP_COUNTER;
      }
    } else {
      Log.e(getString(R.string.app_name), TAG + ": Invalid or missing authority in uri");
      Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
      return;
    }

    user = validateAndGetUserInPath(path);
    if (user == null) {
      Log.e(getString(R.string.app_name), TAG + ": Missing user id in uri");
      Toast.makeText(this, "Invalid QR Code User", Toast.LENGTH_LONG).show();
      return;
    }

    secret = uri.getQueryParameter(SECRET_PARAM);

    if (secret == null || secret.length() == 0) {
      Log.e(getString(R.string.app_name), TAG +
          ": Secret key not found in URI");
      Toast.makeText(this, "Invalid QR Code Secret Null", Toast.LENGTH_LONG).show();
      return;
    }

    if (AccountDb.getSigningOracle(secret) == null) {
      Log.e(getString(R.string.app_name), TAG + ": Invalid secret key");
      Toast.makeText(this, "Invalid QR Code Secret Key", Toast.LENGTH_LONG).show();
      return;
    }

    if (secret.equals(mAccountDb.getSecret(user)) &&
        counter == mAccountDb.getCounter(user) &&
        type == mAccountDb.getType(user)) {
      return;  // nothing to update.
    }

    etSecretKey.setText(secret);
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
    mAccountDb.close();
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
    private boolean hotpCodeGenerationAllowed;
  }

  /**
   * Register User: Register User with Mobile No. to get the Secret Key for HOTP
   * <p/>
   * Request:
   * JSONObject={"API":"RU",
   * "MDN":"9876543210"}
   * Response:
   * JSONObject={"API":true,
   * "DB": // Unused till now
   * "MSG":"Key Sent to Mobile No. 9876543210",
   * "ET":2.0987,
   * "ST":"Wed 20 Aug 08:31:23 PM"}
   */
  private class RegisterButtonListener implements OnClickListener {
    @Override
    public void onClick(View view) {

      etMobileNo.setVisibility(View.GONE);
      btnRegister.setVisibility(View.GONE);
      tvLoginMessage.setText(getText(R.string.login_wait_message));
      pbLoginWait.setVisibility(View.VISIBLE);
      mUser.user = etMobileNo.getText().toString();

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
              if (response.optBoolean(DashAIO.KEY_API)) {
                etSecretKey.setVisibility(View.VISIBLE);
                btnSaveKey.setVisibility(View.VISIBLE);
                tvOTP.setVisibility(View.VISIBLE);
                btnVerifyOTP.setVisibility(View.VISIBLE);
                btnScanOR.setVisibility(View.VISIBLE);
              }
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
      data.putExtra(DashAIO.PREF_KEY_UserID, mUser.user);
      try {
        JSONObject userData=apiRespUserStat.getJSONObject("DB").getJSONObject("USER");
        data.putExtra(DashAIO.PREF_KEY_NAME, userData.optString("UserName"));
        data.putExtra(DashAIO.PREF_KEY_POST, userData.optString("Designation"));
        data.putExtra(DashAIO.PREF_KEY_EMAIL, userData.optString("eMailID"));
      } catch (JSONException e) {
        e.printStackTrace();
        Toast.makeText(getApplicationContext(),"Profile Error:"+e.getMessage(),Toast.LENGTH_LONG).show();
      }
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
   * Listener for the Button that generates the next OTP value.
   * <p/>
   * Request:
   * JSONObject={"API":"RU",
   * "MDN":"9876543210"}
   * <p/>
   * Response:
   * JSONObject={"API":true,
   * "DB": {'KeyUpdated':1,
   * "USER":{"UserName":"", TODO Store User Profile Data received from server.
   * "Designation":"",
   * "eMailID":""}
   * }
   * "MSG":"Mobile No. 9876543210 is Registered Successfully.",
   * "ET":2.0987,
   * "ST":"Wed 20 Aug 08:31:23 PM"}
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
              if (response.optBoolean(DashAIO.KEY_API)) {
                etSecretKey.setVisibility(View.GONE);
                btnStartMessaging.setVisibility(View.VISIBLE);
                btnSaveKey.setVisibility(View.GONE);
                btnVerifyOTP.setVisibility(View.GONE);
                tvOTP.setVisibility(View.GONE);
                btnScanOR.setVisibility(View.GONE);
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
          HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
      );
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
          HOTP_DISPLAY_TIMEOUT
      );
    }
  }
}
