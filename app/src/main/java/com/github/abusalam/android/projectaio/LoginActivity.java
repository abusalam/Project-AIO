package com.github.abusalam.android.projectaio;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.sms.GroupSMS;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends ActionBarActivity {

    private static final String TAG=LoginActivity.class.getSimpleName();
    protected EditText etMobileNo;
    protected RequestQueue rQueue;

    View.OnClickListener btnUpdateClick=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // WebServer Request URL
            //String serverURL = "http://echo.jsontest.com/key/value/one/two";
            //String serverURL = "http://10.42.0.1/apps/android/api.php";
            String serverURL = "http://www.paschimmedinipur.gov.in/apps/android/api.php";

            JSONObject jsonPost=new JSONObject();

            try {
                jsonPost.put("API","RU");
                jsonPost.put("mdn",etMobileNo.getText());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    serverURL, jsonPost,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, response.toString());
                            Toast.makeText(getApplicationContext(), response.optString("Status"), Toast.LENGTH_SHORT).show();
                            Intent data=new Intent();
                            data.putExtra(DashAIO.PREF_KEY_UserID,response.optString("UserID"));
                            data.putExtra(DashAIO.PREF_KEY_Secret,response.optString("SentOn"));
                            setResult(RESULT_OK,data);
                            finish();
                        }
                    }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    String msgError="Error: " + error.getMessage();
                    VolleyLog.d(TAG, msgError);
                    Toast.makeText(getApplicationContext(), msgError, Toast.LENGTH_SHORT).show();
                }
            }
            );

            // Adding request to request queue
            jsonObjReq.setTag(TAG);
            rQueue.add(jsonObjReq);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etMobileNo=(EditText) findViewById(R.id.etUserMobile);
        rQueue = VolleyAPI.getInstance(this).getRequestQueue();
        ImageButton GetImgButton = (ImageButton) findViewById(R.id.btnUpdateProfile);
        GetImgButton.setOnClickListener(btnUpdateClick);
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
