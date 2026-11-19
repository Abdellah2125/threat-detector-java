package com.threatdetector.runtime;

/** A check that could not be completed (API key, 429, timeout, bad response...). */
public class CheckUnavailableException extends Exception {
    private final String code;

    public CheckUnavailableException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
