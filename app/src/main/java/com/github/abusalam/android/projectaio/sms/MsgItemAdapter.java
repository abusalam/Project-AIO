package com.github.abusalam.android.projectaio.sms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.abusalam.android.projectaio.R;

import java.util.ArrayList;

/**
 * Created by abu on 12/8/14.
 */
public class MsgItemAdapter extends ArrayAdapter<MsgItem> {

  ArrayList<MsgItem> msgItems;
  int resource;

  public MsgItemAdapter(Context context, int resource, ArrayList<MsgItem> msgItems) {
    super(context, resource, msgItems);
    this.resource = resource;
    this.msgItems = msgItems;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LinearLayout MsgItemView;
    MsgItem msgItem = getItem(position);

    if (convertView == null) {
      MsgItemView = new LinearLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater) getContext().getSystemService(inflater);
      li.inflate(resource, MsgItemView, true);
    } else {
      MsgItemView = (LinearLayout) convertView;
    }

    String msgSentTo = msgItem.getSentTo();
    String msgText = msgItem.getMsgText();
    String msgStatus = msgItem.getMsgStatus();
    boolean isShowPB = msgItem.isShowPB();

    TextView tvSentTo = (TextView) MsgItemView.findViewById(R.id.tvSentTo);
    TextView tvMsgShow = (TextView) MsgItemView.findViewById(R.id.tvMsgShow);
    TextView tvMsgStatus = (TextView) MsgItemView.findViewById(R.id.tvMsgStatus);
    ProgressBar pbMsg = (ProgressBar) MsgItemView.findViewById(R.id.pbMsg);

    tvSentTo.setText(msgSentTo);
    tvMsgShow.setText(msgText);
    tvMsgStatus.setText(msgStatus);
    if (isShowPB) {
      pbMsg.setVisibility(View.VISIBLE);
      pbMsg.setIndeterminate(true);
    } else {
      pbMsg.setVisibility(View.GONE);
    }

    //Manufacture New item if going to exhaust
    if (this.msgItems.size() < (position + 2)) {
      new LoadSMS().execute(this.msgItems.size() + 2);
    }

    return MsgItemView;
  }

}
