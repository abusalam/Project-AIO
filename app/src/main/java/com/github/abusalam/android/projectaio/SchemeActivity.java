package com.github.abusalam.android.projectaio;


import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.mpr.Scheme;
import com.github.abusalam.android.projectaio.mpr.SchemeAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;



public class SchemeActivity extends ActionBarActivity {
    public static final String TAG = SchemeActivity.class.getSimpleName();
    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
     * generated for an account until the moment the next code can be generated for the account.
     * This is to prevent the user from generating too many HOTP codes in a short period of time.
     */

    static final String API_URL = "http://10.173.168.169/apps/mpr/AndroidAPI.php";

    private JSONArray respJsonArray;
    private RequestQueue rQueue;
private                             ArrayList<Scheme> SchemeList;
    private ListView lvSchemes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme);

        rQueue = VolleyAPI.getInstance(this).getRequestQueue();
        lvSchemes = (ListView) findViewById(R.id.lvSchemes);

        lvSchemes.setOnItemClickListener(new SelectSchemeClickListener());
        SchemeList = new ArrayList<Scheme>();

        getUserSchemes();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scheme, menu);
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

    private void getUserSchemes() {

        final JSONObject jsonPost = new JSONObject();

        try {
            jsonPost.put("API", "US");
            jsonPost.put("UID", "5"); // TODO Supply Dynamic UserMapID instead of Static
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                API_URL, jsonPost,
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
                                Scheme mScheme = new Scheme();
                                mScheme.setSchemeID(respJsonArray.getJSONObject(i).getInt("ID"));
                                mScheme.setSchemeName(respJsonArray.getJSONObject(i).optString("SN"));
                                SchemeList.add(mScheme);
                            }
                            // Spinner adapter
                            lvSchemes.setAdapter(new SchemeAdapter(SchemeActivity.this,
                                    R.layout.scheme_view, SchemeList));
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

    private class SelectSchemeClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            Toast.makeText(getApplicationContext(), "Scheme ID: " + SchemeList.get(i).getSchemeID(), Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(getApplicationContext(), SchemeActivity.class));

        }
    }
}
