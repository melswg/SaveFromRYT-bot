package ru.malik.savefrom.service;

public class ContentTooLargeException extends RuntimeException {
    public ContentTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
