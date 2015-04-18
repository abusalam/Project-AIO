package com.github.abusalam.android.projectaio.mpr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.abusalam.android.projectaio.R;

import java.util.ArrayList;


public class UserAdapter extends ArrayAdapter<User> {

  ArrayList<User> Users;
  int resource;

  public UserAdapter(Context context, int resource, ArrayList<User> Users) {
    super(context, resource, Users);
    this.resource = resource;
    this.Users = Users;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    RelativeLayout UserView;
    User mUsers = getItem(position);

    if (convertView == null) {
      UserView = new RelativeLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, UserView, true);
    } else {
      UserView = (RelativeLayout) convertView;
    }
    TextView tvUserName = (TextView) UserView.findViewById(R.id.tvUserName);
    TextView tvBalance = (TextView) UserView.findViewById(R.id.tvBalance);
    TextView tvSanctioned = (TextView) UserView.findViewById(R.id.tvSanctioned);
    TextView tvSchemes = (TextView) UserView.findViewById(R.id.tvSchemes);

    tvBalance.setText("₹ " + mUsers.getBalance());
    tvUserName.setText(mUsers.getUserName());
    tvSanctioned.setText("₹ " + mUsers.getSanctions());
    tvSchemes.setText("[" + mUsers.getUserID() + "] " + mUsers.getMobileNo());

    return UserView;
  }

}
