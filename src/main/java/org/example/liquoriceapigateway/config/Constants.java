package org.example.liquoriceapigateway.config;

import io.jsonwebtoken.SignatureAlgorithm;

public class Constants {
    public final static int JWT_ACCESS_TOKEN_SECONDS_TIMEOUT_SKEW = 10;
    public static final SignatureAlgorithm JWT_SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    public final static String BASE_PATH = "/api/v1";


}
