package com.github.abusalam.android.projectaio.sms;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
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
import com.github.abusalam.android.projectaio.DashAIO;
import com.github.abusalam.android.projectaio.R;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class GroupFragment extends ListFragment {

  public static final String TAG = "GroupFragment";
  static final String API_URL = DashAIO.API_HOST + "/apps/smsgw/android/api.php";

  private OnListFragmentInteractionListener mListener;

  private JSONArray respJsonArray;
  private RequestQueue rQueue;

  private ProgressBar progressBar;

  private ArrayList<Group> GroupList;
  /**
   * The fragment's ListView/GridView.
   */
  private ListView mListView;

  /**
   * The Adapter which will be used to populate the ListView/GridView with
   * Views.
   */
  private GroupAdapter mAdapter;

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "MDN";
  private static final String ARG_PARAM2 = "OTP";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  // TODO: Rename and change types of parameters
  public static GroupFragment newInstance(String MDN, String OTP) {
    GroupFragment fragment = new GroupFragment();
    Bundle args = new Bundle();
    args.putString(ARG_PARAM1, MDN);
    args.putString(ARG_PARAM2, OTP);
    fragment.setArguments(args);
    return fragment;
  }

  public GroupFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments() != null) {
      mParam1 = getArguments().getString(ARG_PARAM1);
      mParam2 = getArguments().getString(ARG_PARAM2);
    }

    GroupList = new ArrayList<>();

    // TODO: Change Adapter to display your content
    mAdapter = new GroupAdapter(getActivity(),
      R.layout.group_view, GroupList);

    rQueue = VolleyAPI.getInstance(getActivity()).getRequestQueue();

    //if (savedInstanceState == null) {
    getGroups(mParam1, mParam2);
    //}
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_groups, container, false);

    // Set the adapter
    mListView = (ListView) view.findViewById(android.R.id.list);
    mListView.setAdapter(mAdapter);

    // Set OnItemClickListener so we can be notified on item clicks
    //mListView.setOnItemClickListener(this);
    progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

    if (savedInstanceState == null) {
      progressBar.setVisibility(View.GONE);
    }

    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnListFragmentInteractionListener) {
      mListener = (OnListFragmentInteractionListener) activity;
    } else {
      throw new RuntimeException(activity.toString()
        + " must implement OnListFragmentInteractionListener");
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    mListener.onListFragmentInteraction(mAdapter.getItem(position));
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnListFragmentInteractionListener {
    // TODO: Update argument type and name
    void onListFragmentInteraction(Group item);
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

  private void getGroups(String MDN, String OTP) {

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "CG");
      jsonPost.put("MDN", MDN);
      jsonPost.put("OTP", OTP);
      Log.e(TAG, "Mobile: " + MDN + " OTP: " + OTP);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          Log.e(TAG, "UserGroups: " + response.toString());
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              Group mGroup = new Group();
              mGroup.setGroupID(respJsonArray.getJSONObject(i).getInt("GroupID"));
              mGroup.setGroupName(respJsonArray.getJSONObject(i).optString("GroupName"));
              GroupList.add(mGroup);
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
        Log.e(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All GroupActivity Please Wait...", Toast.LENGTH_SHORT).show();
  }
}
