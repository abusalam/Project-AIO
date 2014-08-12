package com.github.abusalam.android.projectaio.sms;


public class MsgItem {

    private String SentTo;
    private String MsgText;
    private String MsgStatus;

    private boolean ShowPB;

    public void setShowPB(boolean showPB) {
        ShowPB = showPB;
    }

    public boolean isShowPB() {
        return ShowPB;
    }

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

    /**
     * ******** Set Methods *****************
     */

    public void setSentTo(String SentTo) {
        this.SentTo = SentTo;
    }

    public void setMsgText(String MsgText) {
        this.MsgText = MsgText;
    }

    public void setMsgStatus(String MsgStatus) {
        this.MsgStatus = MsgStatus;
    }

    /**
     * ******** Get Methods ***************
     */

    public String getSentTo() {
        return this.SentTo;
    }

    public String getMsgText() {
        return this.MsgText;
    }

    public String getMsgStatus() {
        return this.MsgStatus;
    }
}
