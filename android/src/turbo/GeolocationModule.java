package com.reactnativecommunity.geolocation;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;

public class GeolocationModule extends GeolocationModuleSpec {
    public static final String NAME = GeolocationModule.NAME;
    GeolocationModuleImpl mImpl;
  
    GeolocationModule(ReactApplicationContext context) {
      super(context);
      mImpl = new GeolocationModuleImpl(context);
    }
  
    @Override
    @NonNull
    public String getName() {
      return GeolocationModuleImpl.NAME;
    }
  
    @Override
    @ReactMethod
    public void getCurrentPosition(
        final ReadableMap options,
        final Callback success,
        final Callback error) {
          mImpl.getCurrentPosition(options, success, error);
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
  