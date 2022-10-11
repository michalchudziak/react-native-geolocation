package com.reactnativecommunity.geolocation;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;

public class RNCGeolocationModule extends ReactContextBaseJavaModule {
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

    @ReactMethod
    public void setConfiguration(ReadableMap config) {
      mImpl.setConfiguration(config);
    }

    @ReactMethod
    public void requestAuthorization(final Callback success, final Callback error) { 
      mImpl.requestAuthorization(success, error);
    }
  
    @ReactMethod
    public void getCurrentPosition(
        final ReadableMap options,
        final Callback position,
        final Callback error) {
          mImpl.getCurrentPosition(options, position, error);
    }

    @ReactMethod
    public void startObserving(ReadableMap options) {
      mImpl.startObserving(options);
    }

    @ReactMethod
    public void stopObserving() {
      mImpl.stopObserving();
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Keep: Required for RN RCTEventEmitter class (iOS).
    }

    @ReactMethod
    public void removeListeners(double count) {
        // Keep: Required for RN RCTEventEmitter class (iOS).
    }
  }
  