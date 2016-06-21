package com.github.abusalam.android.projectaio.sms;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class Contact {

  private long ContactID;
  private String ContactName;
  private String Designation;
  private String MobileNo;

  public long getContactID() {
    return ContactID;
  }

  public void setContactID(long contactID) {
    ContactID = contactID;
  }

  public String getContactName() {
    return ContactName;
  }

  public void setContactName(String contactName) {
    ContactName = contactName;
  }

  public String getDesignation() {
    return Designation;
  }

  public void setDesignation(String designation) {
    Designation = designation;
  }

  public String getMobileNo() {
    return MobileNo;
  }

  public void setMobileNo(String mobileNo) {
    MobileNo = mobileNo;
  }
}
