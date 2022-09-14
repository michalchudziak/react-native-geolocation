package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

@SuppressLint("MissingPermission")
public class PlayServicesLocationManager extends BaseLocationManager {
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    protected PlayServicesLocationManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext.getCurrentActivity());
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    emitError(PositionError.POSITION_UNAVAILABLE, "No location provided by FusedLocationProviderClient.");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("geolocationDidChange", locationToMap(location));
                }
            }
        };
    }

    @Override
    public void getCurrentLocationData(ReadableMap options, Callback success, Callback error) {
        AndroidLocationManager.LocationOptions locationOptions = AndroidLocationManager.LocationOptions.fromReactMap(options);

        try {
            mFusedLocationClient.getCurrentLocation(locationOptions.highAccuracy ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_LOW_POWER, null)
                    .addOnSuccessListener(mReactContext.getCurrentActivity(), location -> {
                        if (location != null) {
                            if ((SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                                success.invoke(locationToMap(location));
                            }
                        } else {
                            error.invoke(PositionError.buildError(
                                    PositionError.POSITION_UNAVAILABLE, "No location provided by FusedLocationProviderClient.")
                            );
                        }
                    });
        } catch (SecurityException e) {
            throw e;
        }
    }

    @Override
    public void startObserving(ReadableMap options) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(locationOptions.interval);
        if (locationOptions.fastestInterval >= 0) {
            locationRequest.setFastestInterval(locationOptions.fastestInterval);
        }
        locationRequest.setExpirationDuration((long) locationOptions.maximumAge);
        locationRequest.setSmallestDisplacement(locationOptions.distanceFilter);
        locationRequest.setPriority(
                locationOptions.highAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_LOW_POWER
        );
        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            throw e;
        }
    }

    @Override
    public void stopObserving() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}
