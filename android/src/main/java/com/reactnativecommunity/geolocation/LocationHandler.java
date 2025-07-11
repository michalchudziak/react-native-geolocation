package com.reactnativecommunity.geolocation;

import com.facebook.react.bridge.Callback;

public interface LocationHandler {
    void getCurrentLocation(LocationOptions options, Callback success, Callback error);

    void startLocationUpdates(LocationOptions options);

    void stopLocationUpdates();
}



