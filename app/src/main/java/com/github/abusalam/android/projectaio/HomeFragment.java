package com.github.abusalam.android.projectaio;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.ajax.NetConnection;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.mpr.Scheme;
import com.github.abusalam.android.projectaio.mpr.SchemeAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class HomeFragment extends Fragment {

  public static final String TAG = "MprFragment";
  static final String API_URL = DashAIO.API_HOST + "/apps/mpr/android/api.php";
  static final String UID = "UID";
  static final String SID = "ID";
  static final String SN = "SN";

  private JSONArray respJsonArray;
  private RequestQueue rQueue;

  private ProgressBar progressBar;

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  private ArrayList<Scheme> SchemeList;
  /**
   * The fragment's ListView/GridView.
   */
  private ListView mListView;

  /**
   * The Adapter which will be used to populate the ListView/GridView with
   * Views.
   */
  private SchemeAdapter mAdapter;

  // TODO: Rename and change types of parameters
  public static HomeFragment newInstance(String param1, String param2) {
    HomeFragment fragment = new HomeFragment();
    Bundle args = new Bundle();
    args.putString(ARG_PARAM1, param1);
    args.putString(ARG_PARAM2, param2);
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public HomeFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments() != null) {
      mParam1 = getArguments().getString(ARG_PARAM1);
      mParam2 = getArguments().getString(ARG_PARAM2);
    }

    SchemeList = new ArrayList<>();

    // TODO: Change Adapter to display your content
    mAdapter = new SchemeAdapter(getActivity(),
      R.layout.scheme_view, SchemeList);

    rQueue = VolleyAPI.getInstance(getActivity()).getRequestQueue();

    getUserSchemes(mParam1);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_home, container, false);

    // Set the adapter
    mListView = (ListView) view.findViewById(android.R.id.list);
    mListView.setAdapter(mAdapter);

    // Set OnItemClickListener so we can be notified on item clicks
    //mListView.setOnItemClickListener(this);

    progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  /**
   * The default content for this Fragment has a TextView that is shown when
   * the list is empty. If you would like to change the text, call this method
   * to supply the text it should use.
   */
  public void setEmptyText(CharSequence emptyText) {
    View emptyView = mListView.getEmptyView();

    if (emptyView instanceof TextView) {
      ((TextView) emptyView).setText(emptyText);
    }
  }

  private void getUserSchemes(String UID) {

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "US");
      jsonPost.put("UID", UID);
      //Log.d(TAG, "UserMapID: " + UID);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          //Log.d(TAG, "UserSchemes: " + response.toString());
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              Scheme mScheme = new Scheme();
              mScheme.setSchemeID(respJsonArray.getJSONObject(i).getInt("ID"));
              mScheme.setSchemeName(respJsonArray.getJSONObject(i).optString("SN"));
              SchemeList.add(mScheme);
            }
            mAdapter.notifyDataSetChanged();
          } catch (JSONException e) {
            e.printStackTrace();
          }
          progressBar.setVisibility(View.GONE);
        }
      }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        //Log.d(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    jsonObjReq.setShouldCache(false);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All Schemes Please Wait...", Toast.LENGTH_SHORT).show();
  }
}
