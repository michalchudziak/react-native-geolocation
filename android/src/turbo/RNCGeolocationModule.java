package com.reactnativecommunity.geolocation;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class RNCGeolocationModule extends NativeRNCGeolocationSpec {
    public static final String NAME = GeolocationModule.NAME;
    GeolocationModule mImpl;
  
    RNCGeolocationModule(ReactApplicationContext context) {
      super(context);
      mImpl = new GeolocationModule(context);
    }

    @Override
    @NonNull
    public String getName() {
      return GeolocationModule.NAME;
    }

    @Override
    @ReactMethod
    public void setConfiguration(ReadableMap config) {
        mImpl.setConfiguration(config);
    }

    @Override
    @ReactMethod
    public void requestAuthorization(Callback success, Callback error) {
        mImpl.requestAuthorization(success, error);
    }

    @Override
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, Callback position, Callback error) {
        mImpl.getCurrentPosition(options, position, error);
    }

    @Override
    @ReactMethod
    public void startObserving(ReadableMap options) {
      mImpl.startObserving(options);
    }

    @Override
    @ReactMethod
    public void stopObserving() {
      mImpl.stopObserving();
    }

    @Override
    public void addListener(String eventName) {
        // Keep: Required for RN RCTEventEmitter class (iOS).
    }

    @Override
    public void removeListeners(double count) {
        // Keep: Required for RN RCTEventEmitter class (iOS).
    }
  }
  