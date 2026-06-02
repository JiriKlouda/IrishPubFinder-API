package com.irishpubfinder.api.exception;

public class DuplicateFavouriteException extends RuntimeException {
    public DuplicateFavouriteException(String message) {
        super(message);
    }
}
