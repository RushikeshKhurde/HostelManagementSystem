package com.hostel.management.dto;

public final class ValidationConstants {

    private ValidationConstants() {}

    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$";
    public static final String PASSWORD_MESSAGE = "Password must be 8-20 characters and include an uppercase letter, a lowercase letter, a digit, and a special character (@#$%^&+=!)";

    public static final String MOBILE_PATTERN = "^[6-9]\\d{9}$";
    public static final String MOBILE_MESSAGE = "Enter a valid 10-digit mobile number starting with 6-9";
}
