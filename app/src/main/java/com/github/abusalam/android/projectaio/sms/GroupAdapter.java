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


public class GroupAdapter extends ArrayAdapter<Group> {

  ArrayList<Group> Groups;
  int resource;
  Context mContext;

  public GroupAdapter(Context context, int resource, ArrayList<Group> Groups) {
    super(context, resource, Groups);
    this.resource = resource;
    this.Groups = Groups;
    this.mContext = context;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LinearLayout GroupView;
    Group mGroup = getItem(position);

    if (convertView == null) {
      GroupView = new LinearLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, GroupView, true);
    } else {
      GroupView = (LinearLayout) convertView;
    }

    Long lngGroupID = mGroup.getGroupID();
    String txtGroupName = mGroup.getGroupName();

    TextView tvGroupID = (TextView) GroupView.findViewById(R.id.tvGroupID);
    TextView tvGroupName = (TextView) GroupView.findViewById(R.id.tvGroupName);

    tvGroupID.setText(this.mContext.getString(R.string.lbl_group_id) + " " + lngGroupID.toString());
    tvGroupName.setText(txtGroupName);

    return GroupView;
  }

}
