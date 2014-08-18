package com.github.abusalam.android.projectaio.sms;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;



public class GroupSMS extends ActionBarActivity implements OnClickListener {

    public static final String TAG = GroupSMS.class.getSimpleName();

    private SharedPreferences mInSecurePrefs;

    private MsgItemAdapter lvMsgHistAdapter;
    private Spinner spinner;
    private ArrayList<MsgItem> lvMsgContent;
    private MessageDB msgDB;

    protected String appSecret;
    protected String mUserID;

    private EditText etMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        msgDB = new MessageDB(getApplicationContext());
        mInSecurePrefs=getSharedPreferences("mSecrets",MODE_PRIVATE);

        appSecret=mInSecurePrefs.getString("AppSecret",null);
        mUserID=mInSecurePrefs.getString("UserID",null);

        setContentView(R.layout.activity_group_sms);


        spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sms_groups, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        lvMsgContent = new ArrayList<MsgItem>();

        lvMsgContent.addAll(msgDB.getAllSms());
        msgDB.closeDB();
        // TODO: Retrieve & Populate List of SMSs Sent by the user using MessageAPI.

        ListView lvMsgHist = (ListView) findViewById(R.id.lvMsgHist);
        lvMsgHistAdapter = new MsgItemAdapter(this,
                R.layout.msg_item,
                lvMsgContent);

        lvMsgHist.setAdapter(lvMsgHistAdapter);

        etMsg = (EditText) findViewById(R.id.etMsg);

        ImageButton GetAjaxData = (ImageButton) findViewById(R.id.btnSendSMS);

        GetAjaxData.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {

        String txtMsg = etMsg.getText().toString();
        final MsgItem newMsgItem = new MsgItem(spinner.getSelectedItem().toString(), etMsg.getText().toString(), "Sending on Click...");

        if (txtMsg.length() > 0) {
            newMsgItem.setShowPB(true);
            lvMsgContent.add(newMsgItem);
            newMsgItem.setMsgID(msgDB.saveSMS(newMsgItem));
            msgDB.closeDB();
            etMsg.setText("");
        }

        lvMsgHistAdapter.notifyDataSetChanged();

        // WebServer Request URL
        //String serverURL = "http://echo.jsontest.com/key/value/one/two";
        //String serverURL = "http://10.42.0.1/apps/android/api.php";
        String serverURL = "http://www.paschimmedinipur.gov.in/apps/android/api.php";

        RequestQueue queue = VolleyAPI.getInstance(this).getRequestQueue();

        final String tag_json_obj = "json_obj_req";

        JSONObject jsonPost=new JSONObject();

        try {
            jsonPost.put("API","v1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                serverURL, jsonPost,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.d(TAG,"Group-SMS " + response.toString());
                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
                        newMsgItem.setMsgStatus(response.optString("SentOn"));
                        newMsgItem.setShowPB(false);
                        msgDB.updateSMS(newMsgItem);
                        lvMsgHistAdapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
            }
        }
        );

        // Adding request to request queue
        jsonObjReq.setTag(tag_json_obj);
        queue.add(jsonObjReq);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.group_sm, menu);
        return true;
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
