package com.github.abusalam.android.projectaio.sms;


public class MsgItem {

  private long MsgID;
  private String SentTo;
  private String MsgText;
  private String MsgStatus;

  private boolean ShowPB;

  public MsgItem() {
    SentTo = "";
    MsgText = "";
    MsgStatus = "";
  }

  public MsgItem(String sentTo, String msgText, String msgStatus) {
    SentTo = sentTo;
    MsgText = msgText;
    MsgStatus = msgStatus;
  }

  public boolean isShowPB() {
    return ShowPB;
  }

  public void setShowPB(boolean showPB) {
    ShowPB = showPB;
  }

  public long getMsgID() {
    return MsgID;
  }

  public void setMsgID(long msgID) {
    MsgID = msgID;
  }

  /**
   * ******** Get Methods ***************
   */

  public String getSentTo() {
    return this.SentTo;
  }

  /**
   * ******** Set Methods *****************
   */

  public void setSentTo(String SentTo) {
    this.SentTo = SentTo;
  }

  public String getMsgText() {
    return this.MsgText;
  }

  public void setMsgText(String MsgText) {
    this.MsgText = MsgText;
  }

  public String getMsgStatus() {
    return this.MsgStatus;
  }

  public void setMsgStatus(String MsgStatus) {
    this.MsgStatus = MsgStatus;
  }
}
