package com.github.abusalam.android.projectaio;

/**
 * A tuple of user, OTP value, and type, that represents a particular user.
 *
 * @author adhintz@google.com (Drew Hintz)
 */
public class User {
  public String pin; // calculated OTP, or a placeholder if not calculated
  public String UserMapID;
  public String MobileNo;
  public boolean isHotp = true; // used to see if button needs to be displayed

  /**
   * HOTP only: Whether code generation is allowed for this account.
   */
  public boolean hotpCodeGenerationAllowed = true;
}