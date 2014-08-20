package com.github.abusalam.android.projectaio;

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
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.sms.MessageDB;
import com.github.abusalam.android.projectaio.sms.MsgItem;
import com.github.abusalam.android.projectaio.sms.MsgItemAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class GroupSMS extends ActionBarActivity {

    public static final String TAG = GroupSMS.class.getSimpleName();

    private MsgItemAdapter lvMsgHistAdapter;
    private Spinner spinner;
    private ArrayList<MsgItem> lvMsgContent;
    private MessageDB msgDB;
    private RequestQueue rQueue;

    protected String appSecret;
    protected String mUserID;

    private EditText etMsg;

    View.OnClickListener btnSendSMSClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String txtMsg = etMsg.getText().toString();
            final MsgItem newMsgItem = new MsgItem(spinner.getSelectedItem().toString(),
                    etMsg.getText().toString(), getString(R.string.default_msg_status));

            if (txtMsg.length() > 0) {
                newMsgItem.setShowPB(true);
                lvMsgContent.add(newMsgItem);
                newMsgItem.setMsgID(msgDB.saveSMS(newMsgItem));
                msgDB.closeDB();
                etMsg.setText("");
            }

            lvMsgHistAdapter.notifyDataSetChanged();

            final String tag_json_obj = "json_obj_req";

            JSONObject jsonPost = new JSONObject();

            try {
                jsonPost.put("API", "v1");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    DashAIO.API_URL, jsonPost,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "Group-SMS " + response.toString());
                            Toast.makeText(getApplicationContext(), DashAIO.KEY_STATUS, Toast.LENGTH_SHORT).show();
                            newMsgItem.setMsgStatus(response.optString(DashAIO.KEY_SENT_ON));
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
            rQueue.add(jsonObjReq);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rQueue = VolleyAPI.getInstance(this).getRequestQueue();
        msgDB = new MessageDB(getApplicationContext());

        SharedPreferences mInSecurePrefs;
        mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

        appSecret = mInSecurePrefs.getString(DashAIO.PREF_KEY_Secret, null);
        mUserID = mInSecurePrefs.getString(DashAIO.PREF_KEY_UserID, null);

        setContentView(R.layout.activity_group_sms);

        spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sms_groups, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // TODO: Retrieve & Populate List of SMSs Sent by the user using MessageAPI.
        lvMsgContent = new ArrayList<MsgItem>();
        lvMsgContent.addAll(msgDB.getAllSms());
        msgDB.closeDB();

        ListView lvMsgHist = (ListView) findViewById(R.id.lvMsgHist);
        lvMsgHistAdapter = new MsgItemAdapter(this, R.layout.msg_item, lvMsgContent);
        lvMsgHist.setAdapter(lvMsgHistAdapter);

        etMsg = (EditText) findViewById(R.id.etMsg);

        ImageButton btnSendSMS = (ImageButton) findViewById(R.id.btnSendSMS);
        btnSendSMS.setOnClickListener(btnSendSMSClick);
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
