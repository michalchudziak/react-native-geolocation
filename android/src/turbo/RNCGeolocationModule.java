package com.reactnativecommunity.geolocation;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
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
    public void setConfiguration(ReadableMap config) { }

    @Override
    public void requestAuthorization() { }

    @Override
    @ReactMethod
    public void getCurrentPosition(ReadableMap options, Callback position, Callback error) {
        mImpl.getCurrentPosition(options, position, error);
    }

    @Override
    @NonNull
    public String getName() {
      return GeolocationModule.NAME;
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
  }
  