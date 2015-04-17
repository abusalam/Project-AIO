package com.github.abusalam.android.projectaio.mpr;

public class User {
    private int UserID;
    private String UserName;
    private String MobileNo;
    private long Balance;
    private long Sanctions;
    private String Schemes;

    public User() {
        Balance = 0;
    }

    public User(int userID, String userName, String mobileNo) {
        UserID = userID;
        UserName = userName;
        MobileNo = mobileNo;
        Balance = 0;
    }

    public long getSanctions() {
        return Sanctions;
    }

    public void setSanctions(long sanctions) {
        Sanctions = sanctions;
    }

    public String getSchemes() {
        return Schemes;
    }

    public void setSchemes(String schemes) {
        Schemes = schemes;
    }

    public long getBalance() {
        return Balance;
    }

    public void setBalance(long balance) {
        Balance = balance;
    }

    public int getUserID() {
        return UserID;
    }

    public void setUserID(int userID) {
        UserID = userID;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getMobileNo() {
        return MobileNo;
    }

    public void setMobileNo(String mobileNo) {
        MobileNo = mobileNo;
    }
}
