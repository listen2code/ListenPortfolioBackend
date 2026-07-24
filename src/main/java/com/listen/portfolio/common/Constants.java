package com.listen.portfolio.common;

public class Constants {
    // Password constants
    public static final String DEFAULT_RESET_PASSWORD = "888888";
    
    // Error codes
    public static final String DEFAULT_SERVER_ERROR = "1";
    
    // Auth error codes
    public static final String ERR_INVALID_CREDENTIALS = "AUTH_0300";
    public static final String ERR_ACCOUNT_NOT_FOUND = "AUTH_0301";
    public static final String ERR_ACCESS_DENIED = "AUTH_0305";
    
    // Business error codes
    public static final String ERR_USERNAME_EXISTS = "BIZ_0500";
    public static final String ERR_CURRENT_PASSWORD_INCORRECT = "BIZ_0502";
    public static final String ERR_DELETE_ACCOUNT_FAILED = "BIZ_0503";
    public static final String ERR_LOGOUT_FAILED = "BIZ_0504";
    public static final String ERR_ABOUT_ME_NOT_FOUND = "BIZ_0505";
    public static final String ERR_PROJECTS_NOT_FOUND = "BIZ_0506";
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
