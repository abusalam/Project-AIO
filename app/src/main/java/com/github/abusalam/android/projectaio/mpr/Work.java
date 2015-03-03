package com.github.abusalam.android.projectaio.mpr;

public class Work {
    private long WorkID;
    private String WorkName;
    private long Balance;

    public long getBalance() {
        return Balance;
    }

    public void setBalance(long balance) {
        Balance = balance;
    }

    public long getWorkID() {
        return WorkID;
    }

    public void setWorkID(long workID) {
        WorkID = workID;
    }

    public String getWorkName() {
        return WorkName;
    }

    public void setWorkName(String workName) {
        WorkName = workName;
    }
}
