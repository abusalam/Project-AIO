package com.github.abusalam.android.projectaio;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A tuple of user, OTP value, and type, that represents a particular user.
 *
 * @author adhintz@google.com (Drew Hintz)
 */
public class User implements Parcelable {
  public static final Parcelable.Creator<User> CREATOR
    = new Parcelable.Creator<User>() {
    public User createFromParcel(Parcel in) {
      return new User(in);
    }

    public User[] newArray(int size) {
      return new User[size];
    }
  };
  public String pin; // calculated OTP, or a placeholder if not calculated
  public String UserMapID;
  public String MobileNo;
  public boolean isHotp = true; // used to see if button needs to be displayed
  /**
   * HOTP only: Whether code generation is allowed for this account.
   */
  public boolean hotpCodeGenerationAllowed = true;

  public User() {
  }

  protected User(Parcel in) {
    /**
     * Order of assignment should be same as writeToParcel()
     */

    pin = in.readString();
    UserMapID = in.readString();
    MobileNo = in.readString();

  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(pin);
    dest.writeString(UserMapID);
    dest.writeString(MobileNo);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public String getPin() {
    return pin;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  public String getUserMapID() {
    return UserMapID;
  }

  public void setUserMapID(String userMapID) {
    UserMapID = userMapID;
  }

  public String getMobileNo() {
    return MobileNo;
  }

  public void setMobileNo(String mobileNo) {
    MobileNo = mobileNo;
  }
}