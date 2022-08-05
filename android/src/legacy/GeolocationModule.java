package com.reactnativecommunity.geolocation;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;

public class GeolocationModule extends ReactContextBaseJavaModule {
    public static final String NAME = NewBobGeo2ModuleImpl.NAME;
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
  
    @ReactMethod
    public void getCurrentPosition(
        final ReadableMap options,
        final Callback success,
        final Callback error) {
          mImpl.getCurrentPosition(options, success, error);
    }

    @ReactMethod
    public void startObserving(ReadableMap options) {
      mImpl.startObserving(options);
    }

    @ReactMethod
    public void stopObserving() {
      mImpl.stopObserving();
    }
  }
  