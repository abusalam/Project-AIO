package com.github.abusalam.android.projectaio.sms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;


/**
 * Created by abu on 17/8/14.
 */
public class MessageDB extends SQLiteOpenHelper {

  private static final String LOG = MessageDB.class.getName();
  private static final String DATABASE_NAME = "MessageDB.db";
  private static final int DATABASE_VERSION = 2;

  // Table names
  private static final String TABLE_SMS = "SMS";

  // Table Columns names
  private static final String _SmsID = "SmsID";
  private static final String _MobileNo = "MobileNo";
  private static final String _Msg = "Msg";
  private static final String _SentOn = "SentOn";
  private static final String _DlrStatus = "Dlr";

  // Database creation sql statement
  private static final String DATABASE_CREATE_SMS = "create table "
      + TABLE_SMS + "("
      + _SmsID + " integer primary key autoincrement, "
      + _Msg + " text,"
      + _MobileNo + " text,"
      + _SentOn + " timestamp not null,"
      + _DlrStatus + " text);";

  public MessageDB(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(DATABASE_CREATE_SMS);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    // on upgrade drop older tables
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);

    // create new tables
    onCreate(sqLiteDatabase);
  }

  public long saveSMS(MsgItem msgItem) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(_Msg, msgItem.getMsgText());
    values.put(_MobileNo, msgItem.getSentTo());
    values.put(_SentOn, System.currentTimeMillis());
    values.put(_DlrStatus, "Submitted");
    // insert row
    long SmsID = db.insert(TABLE_SMS, null, values);
    return SmsID;
  }

  public ArrayList<MsgItem> getAllSms() {
    ArrayList<MsgItem> msgItems = new ArrayList<MsgItem>();
    String selectQuery = "SELECT  * FROM " + TABLE_SMS;

    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.rawQuery(selectQuery, null);

    // looping through all rows and adding to list
    if (c.moveToFirst()) {
      do {
        MsgItem msgItem = new MsgItem();
        msgItem.setMsgText(c.getString(c.getColumnIndex(_Msg)));
        msgItem.setSentTo(c.getString(c.getColumnIndex(_MobileNo)));
        msgItem.setMsgStatus(c.getString(c.getColumnIndex(_DlrStatus)));

        // adding to MsgItem list
        msgItems.add(msgItem);
      } while (c.moveToNext());
    }
    return msgItems;
  }

  public long updateSMS(MsgItem msgItem) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(_Msg, msgItem.getMsgText());
    values.put(_MobileNo, msgItem.getSentTo());
    values.put(_SentOn, System.currentTimeMillis());
    values.put(_DlrStatus, msgItem.getMsgStatus());
    // insert row
    long rowsUpdated = db.update(TABLE_SMS, values, _SmsID + " = ? ",
        new String[]{Long.toString(msgItem.getMsgID())});
    return rowsUpdated;
  }

  public void closeDB() {
    SQLiteDatabase db = this.getReadableDatabase();
    if (db != null && db.isOpen()) {
      db.close();
    }
  }
}
