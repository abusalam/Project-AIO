package com.github.abusalam.android.projectaio.sms;

import android.os.AsyncTask;

/**
 * Created by abu on 16/8/14.
 */
public class LoadSMS extends AsyncTask<Integer,Void,MsgItem> {

    @Override
    protected MsgItem doInBackground(Integer... integers) {
        return new MsgItem("Recipient...","Sample Message...","Message #"+integers);
    }

    @Override
    protected void onPostExecute(MsgItem msgItem) {
        super.onPostExecute(msgItem);
    }
}
