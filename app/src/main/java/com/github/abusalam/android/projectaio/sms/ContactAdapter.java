package com.github.abusalam.android.projectaio.sms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.abusalam.android.projectaio.R;

import java.util.ArrayList;


public class ContactAdapter extends ArrayAdapter<Contact> {

  ArrayList<Contact> Contacts;
  int resource;
  Context mContext;

  public ContactAdapter(Context context, int resource, ArrayList<Contact> Contacts) {
    super(context, resource, Contacts);
    this.resource = resource;
    this.Contacts = Contacts;
    this.mContext = context;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LinearLayout ContactView;
    Contact mContact = getItem(position);

    if (convertView == null) {
      ContactView = new LinearLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, ContactView, true);
    } else {
      ContactView = (LinearLayout) convertView;
    }

    Long lngContactID = mContact.getContactID();
    String txtContactName = mContact.getContactName();

    TextView tvContactID = (TextView) ContactView.findViewById(R.id.tvContactID);
    TextView tvContactName = (TextView) ContactView.findViewById(R.id.tvContactName);
    TextView tvDesignation = (TextView) ContactView.findViewById(R.id.tvDesignation);
    TextView tvMobileNo = (TextView) ContactView.findViewById(R.id.tvMobileNo);

    tvContactID.setText(this.mContext.getString(R.string.lbl_contact_id) + " " + lngContactID.toString());
    tvContactName.setText(txtContactName);
    tvDesignation.setText(mContact.getDesignation());
    tvMobileNo.setText(mContact.getMobileNo());

    return ContactView;
  }

}
