package com.github.abusalam.android.projectaio.mpr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.abusalam.android.projectaio.R;

import java.util.ArrayList;


public class SchemeAdapter extends ArrayAdapter<Scheme> {

  ArrayList<Scheme> Schemes;
  int resource;

  public SchemeAdapter(Context context, int resource, ArrayList<Scheme> Schemes) {
    super(context, resource, Schemes);
    this.resource = resource;
    this.Schemes = Schemes;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LinearLayout SchemeView;
    Scheme mScheme = getItem(position);

    if (convertView == null) {
      SchemeView = new LinearLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, SchemeView, true);
    } else {
      SchemeView = (LinearLayout) convertView;
    }

    Long lngSchemeID = mScheme.getSchemeID();
    String txtSchemeName = mScheme.getSchemeName();

    TextView tvSchemeID = (TextView) SchemeView.findViewById(R.id.tvSchemeID);
    TextView tvSchemeName = (TextView) SchemeView.findViewById(R.id.tvSchemeName);

    tvSchemeID.setText(lngSchemeID.toString());
    tvSchemeName.setText(txtSchemeName);

    return SchemeView;
  }

}
