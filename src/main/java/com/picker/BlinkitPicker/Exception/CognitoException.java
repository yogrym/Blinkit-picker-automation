package com.picker.BlinkitPicker.Exception;

import lombok.Getter;

@Getter
public class CognitoException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public CognitoException(int statusCode, String responseBody) {
        super("Cognito error [" + statusCode + "]: " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
