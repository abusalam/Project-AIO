package com.github.abusalam.android.projectaio.mpr;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.abusalam.android.projectaio.User;

public class MprUser extends User implements Parcelable {
  public static final Parcelable.Creator<MprUser> CREATOR
    = new Parcelable.Creator<MprUser>() {
    public MprUser createFromParcel(Parcel in) {
      return new MprUser(in);
    }

    public MprUser[] newArray(int size) {
      return new MprUser[size];
    }
  };
  private int UserID;
  private String UserName;
  private String MobileNo;
  private long Balance;
  private long Sanctions;
  private String Schemes;

  public MprUser() {
    Balance = 0;
  }

  public MprUser(int userID, String userName, String mobileNo) {
    UserID = userID;
    UserName = userName;
    MobileNo = mobileNo;
    Balance = 0;
  }

  private MprUser(Parcel in) {
    /**
     * Order of assignment should be same as writeToParcel()
     */
    setUserID(in.readInt());
    setUserName(in.readString());
    setMobileNo(in.readString());
    setBalance(in.readLong());
    setSanctions(in.readLong());
    setSchemes(in.readString());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(getUserID());
    dest.writeString(getUserName());
    dest.writeString(getMobileNo());
    dest.writeLong(getBalance());
    dest.writeLong(getSanctions());
    dest.writeString(getSchemes());
  }

  @Override
  public int describeContents() {
    return 0;
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
