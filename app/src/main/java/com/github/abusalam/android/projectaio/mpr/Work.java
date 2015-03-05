package com.github.abusalam.android.projectaio.mpr;

import android.os.Parcel;
import android.os.Parcelable;

public class Work implements Parcelable {
    public static final Parcelable.Creator<Work> CREATOR
            = new Parcelable.Creator<Work>() {
        public Work createFromParcel(Parcel in) {
            return new Work(in);
        }

        public Work[] newArray(int size) {
            return new Work[size];
        }
    };
    private long WorkID;
    private String WorkName;
    private String Balance;
    private String Funds;
    private int Progress;
    private int initialPrg;
    private String WorkRemarks;

    public Work() {
        initialPrg = 0;
    }

    private Work(Parcel in) {
        /**
         * Order of assignment should be same as writeToParcel()
         */
        setWorkID(in.readLong());
        setWorkName(in.readString());
        setBalance(in.readString());
        setFunds(in.readString());
        int iPrg = in.readInt();
        setProgress(iPrg);
        setInitialPrg(iPrg);
        setWorkRemarks(in.readString());
    }

    private void setInitialPrg(int iPrg) {
        if (initialPrg == 0) {
            initialPrg = iPrg;
        }
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

    public String getBalance() {
        return Balance;
    }

    public void setBalance(String balance) {
        Balance = balance;
    }

    public String getFunds() {
        return Funds;
    }

    public void setFunds(String funds) {
        Funds = funds;
    }

    public int getProgress() {
        return Progress;
    }

    public void setProgress(int progress) {
        if (initialPrg <= progress) {
            Progress = progress;
        } else {
            Progress=initialPrg;
        }
    }

    public String getWorkRemarks() {
        return WorkRemarks;
    }

    public void setWorkRemarks(String workRemarks) {
        WorkRemarks = workRemarks;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int i) {
        p.writeLong(getWorkID());
        p.writeString(getWorkName());
        p.writeString(getBalance());
        p.writeString(getFunds());
        p.writeInt(getProgress());
        p.writeString(getWorkRemarks());
    }

    @Override
    public String toString() {
        return "Work{" +
                "WorkID=" + WorkID +
                ", WorkName='" + WorkName + '\'' +
                ", Balance=" + Balance +
                ", Funds=" + Funds +
                ", Progress=" + Progress +
                ", WorkRemarks='" + WorkRemarks + '\'' +
                '}';
    }
}
