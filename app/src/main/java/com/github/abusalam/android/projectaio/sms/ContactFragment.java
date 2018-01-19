package com.github.abusalam.android.projectaio.sms;

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

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ContactFragment extends Fragment {

  public static final String TAG = "ContactFragment";
  static final String API_URL = DashAIO.API_HOST + "/apps/smsgw/android/api.php";
  static final String UID = "UID";
  static final String SID = "ID";
  static final String SN = "SN";

  private JSONArray respJsonArray;
  private RequestQueue rQueue;

  private ProgressBar progressBar;

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_MDN = "MDN";
  private static final String ARG_OTP = "OTP";
  private static final String ARG_GRP = "GRP";

  // TODO: Rename and change types of parameters
  private String mMDN;
  private String mOTP;
  private String mGRP;

  private ArrayList<Contact> ContactList;
  /**
   * The fragment's ListView/GridView.
   */
  private ListView mListView;

  /**
   * The Adapter which will be used to populate the ListView/GridView with
   * Views.
   */
  private ContactAdapter mAdapter;

  // TODO: Rename and change types of parameters
  public static ContactFragment newInstance(String MDN, String OTP, String GRP) {
    ContactFragment fragment = new ContactFragment();
    Bundle args = new Bundle();
    args.putString(ARG_MDN, MDN);
    args.putString(ARG_OTP, OTP);
    args.putString(ARG_GRP, GRP);
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ContactFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments() != null) {
      mMDN = getArguments().getString(ARG_MDN);
      mOTP = getArguments().getString(ARG_OTP);
      mGRP = getArguments().getString(ARG_GRP);
    }

    ContactList = new ArrayList<>();

    // TODO: Change Adapter to display your content
    mAdapter = new ContactAdapter(getActivity(),
      R.layout.contact_view, ContactList);

    rQueue = VolleyAPI.getInstance(getActivity()).getRequestQueue();

    getUserContacts(mMDN, mOTP, mGRP);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_contacts, container, false);

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

  private void getUserContacts(String MDN, String OTP, String GRP) {

    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put(DashAIO.KEY_API, "GM");
      jsonPost.put("MDN", MDN);
      jsonPost.put("OTP", OTP);
      jsonPost.put("GRP", GRP);

    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
      API_URL, jsonPost,
      new Response.Listener<JSONObject>() {

        @Override
        public void onResponse(JSONObject response) {
          Log.d(TAG, "UserContacts: " + response.toString());
          Toast.makeText(getActivity().getApplicationContext(),
              response.optString(DashAIO.KEY_STATUS),
              Toast.LENGTH_SHORT).show();
          try {
            respJsonArray = response.getJSONArray("DB");
            for (int i = 0; i < respJsonArray.length(); i++) {
              Contact mContact = new Contact();
              mContact.setContactID(respJsonArray.getJSONObject(i).getInt("ContactID"));
              mContact.setContactName(respJsonArray.getJSONObject(i).optString("ContactName"));
              mContact.setDesignation(respJsonArray.getJSONObject(i).optString("Designation"));
              mContact.setMobileNo(respJsonArray.getJSONObject(i).optString("MobileNo"));
              ContactList.add(mContact);
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
        Log.d(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    jsonObjReq.setShouldCache(false);
    rQueue.add(jsonObjReq);
    //Toast.makeText(getApplicationContext(), "Loading All Contacts Please Wait...", Toast.LENGTH_SHORT).show();
  }
}
