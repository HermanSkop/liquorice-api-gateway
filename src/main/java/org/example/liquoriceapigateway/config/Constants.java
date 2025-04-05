package org.example.liquoriceapigateway.config;

import io.jsonwebtoken.SignatureAlgorithm;

public class Constants {
    public final static int JWT_ACCESS_TOKEN_SECONDS_TIMEOUT_SKEW = 10;
    public static final SignatureAlgorithm JWT_SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    public final static String BASE_PATH = "/api/v1";

    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    public static final String PASSWORD_REGEX_MESSAGE = "Password must contain at least one letter, one number, one special character, and be at least 8 characters long.";
}
