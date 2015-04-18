package com.github.abusalam.android.projectaio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MenuAdapter extends ArrayAdapter<AppMenu> {
  ArrayList<AppMenu> mMenuList;
  int resource;

  public MenuAdapter(Context context, int resource, ArrayList<AppMenu> menuList) {
    super(context, resource, menuList);
    this.resource = resource;
    this.mMenuList = menuList;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    RelativeLayout mAppMenuView;
    AppMenu mAppMenu = getItem(position);

    if (convertView == null) {
      mAppMenuView = new RelativeLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, mAppMenuView, true);
    } else {
      mAppMenuView = (RelativeLayout) convertView;
    }

    String mAppMenuCaption = mAppMenu.getCaption();

    TextView tvMenuCaption = (TextView) mAppMenuView.findViewById(R.id.section_label);

    tvMenuCaption.setText(mAppMenuCaption);

    return mAppMenuView;
  }
}
