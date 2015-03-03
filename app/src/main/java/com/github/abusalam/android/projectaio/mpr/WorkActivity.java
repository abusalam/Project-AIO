package com.github.abusalam.android.projectaio.mpr;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.DashAIO;
import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WorkActivity extends ActionBarActivity {
    public static final String TAG = WorkActivity.class.getSimpleName();
    public static final String WorkID = "WorkID";
    public static final String Work = "Work";
    public static final String Progress = "Progress";

    private JSONArray respJsonArray;
    private RequestQueue rQueue;
    private ArrayList<Work> WorkList;
    private ListView lvWorks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);

        rQueue = VolleyAPI.getInstance(this).getRequestQueue();
        lvWorks = (ListView) findViewById(R.id.lvWorks);

        lvWorks.setOnItemClickListener(new SelectWorkClickListener());
        WorkList = new ArrayList<Work>();
        Log.e("Populate Works:", "Found-" + getIntent().getExtras().getLong(SchemeActivity.SID));
        getUserWorks(getIntent().getExtras().getString(SchemeActivity.UID),
                getIntent().getExtras().getLong(SchemeActivity.SID));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_work, menu);
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
        rQueue.cancelAll(TAG);
    }

    private void getUserWorks(String UID,Long SID) {

        final JSONObject jsonPost = new JSONObject();

        try {
            jsonPost.put(DashAIO.KEY_API, "UW");
            jsonPost.put("UID", UID);
            jsonPost.put("SID", SID);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                SchemeActivity.API_URL, jsonPost,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "UserSchemes: " + response.toString());
                        Toast.makeText(getApplicationContext(),
                                response.optString(DashAIO.KEY_STATUS),
                                Toast.LENGTH_SHORT).show();
                        try {
                            respJsonArray = response.getJSONArray("DB");
                            for (int i = 0; i < respJsonArray.length(); i++) {
                                Work mWork = new Work();
                                mWork.setWorkID(respJsonArray.getJSONObject(i).getInt(WorkID));
                                mWork.setWorkName(respJsonArray.getJSONObject(i).optString(Work));
                                mWork.setBalance(respJsonArray.getJSONObject(i).optLong(Progress));
                                WorkList.add(mWork);
                            }
                            // Spinner adapter
                            lvWorks.setAdapter(new WorkAdapter(WorkActivity.this,
                                    R.layout.work_view, WorkList));
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

        // Adding request to request queue
        jsonObjReq.setTag(TAG);
        rQueue.add(jsonObjReq);
        //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
    }

    private class SelectWorkClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            Toast.makeText(getApplicationContext(),
                    "Work ID: " + WorkList.get(i).getWorkID(),
                    Toast.LENGTH_SHORT).show();
            //Intent mWorks = new Intent(getApplicationContext(), WorkActivity.class);
            //mWorks.putExtra(SchemeActivity.SID,WorkList.get(i).getSchemeID());
            //startActivity(mWorks);
        }
    }
}
