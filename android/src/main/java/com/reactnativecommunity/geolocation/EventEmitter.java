package com.reactnativecommunity.geolocation;

public interface EventEmitter {
    void emitError(int code, String message);
    void emit(String message, Object obj);
}