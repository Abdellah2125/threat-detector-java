package com.threatdetector.intake;

/** Raised when a CSV file cannot be opened or read at all. */
public class LoadException extends Exception {
    public LoadException(String message) {
        super(message);
    }

    public LoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
