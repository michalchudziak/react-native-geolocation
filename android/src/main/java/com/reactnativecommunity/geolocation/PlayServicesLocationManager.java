package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.function.Consumer;
import java.util.function.Function;

@SuppressLint("MissingPermission")
public class PlayServicesLocationManager extends BaseLocationManager {
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationCallback mSingleLocationCallback;
    private SettingsClient mLocationServicesSettingsClient;

    protected PlayServicesLocationManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext);
        mLocationServicesSettingsClient = LocationServices.getSettingsClient(reactContext);
    }

    @Override
    public void getCurrentLocationData(ReadableMap options, Callback success, Callback error) {
        AndroidLocationManager.LocationOptions locationOptions = AndroidLocationManager.LocationOptions.fromReactMap(options);

        Activity currentActivity = mReactContext.getCurrentActivity();

        if (currentActivity == null) {
            error.invoke(PositionError.buildError(PositionError.ACTIVITY_NULL, "mReactContext.getCurrentActivity() returned null but should be non-null in getCurrentLocationData"));
            return;
        }

        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(currentActivity, location -> {
                        if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                            success.invoke(locationToMap(location));
                        } else {
                            mSingleLocationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                    if (locationResult == null) {
                                        emitError(PositionError.POSITION_UNAVAILABLE, "No location provided (FusedLocationProvider/lastLocation).");
                                        return;
                                    }

                                    AndroidLocationManager.LocationOptions locationOptions = AndroidLocationManager.LocationOptions.fromReactMap(options);
                                    Location location = locationResult.getLastLocation();
                                    success.invoke(locationToMap(location));

                                    mFusedLocationClient.removeLocationUpdates(mSingleLocationCallback);
                                    mSingleLocationCallback = null;
                                }

                                @Override
                                public void onLocationAvailability(LocationAvailability locationAvailability) {
                                    if (!locationAvailability.isLocationAvailable()) {
                                        emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/lastLocation).");
                                    }
                                }
                            };
                            checkLocationSettings(options, mSingleLocationCallback);
                        }
                    });
        } catch (SecurityException e) {
            throw e;
        }
    }

    @Override
    public void startObserving(ReadableMap options) {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    emitError(PositionError.POSITION_UNAVAILABLE, "No location provided (FusedLocationProvider/observer).");
                    return;
                }

                mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("geolocationDidChange", locationToMap(locationResult.getLastLocation()));
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider).");
                }
            }
        };

        checkLocationSettings(options, mLocationCallback);
    }

    @Override
    public void stopObserving() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void checkLocationSettings(ReadableMap options, LocationCallback locationCallback) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);

        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(locationOptions.interval);
        requestBuilder.setPriority(locationOptions.highAccuracy ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_LOW_POWER);
        requestBuilder.setMaxUpdateAgeMillis((long) locationOptions.maximumAge);

        if (locationOptions.fastestInterval >= 0) {
            requestBuilder.setMinUpdateIntervalMillis(locationOptions.fastestInterval);
        }

        if (locationOptions.distanceFilter >= 0) {
            requestBuilder.setMinUpdateDistanceMeters(locationOptions.distanceFilter);
        }

        LocationRequest locationRequest = requestBuilder.build();

        LocationSettingsRequest.Builder settingsBuilder = new LocationSettingsRequest.Builder();
        settingsBuilder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = settingsBuilder.build();

        mLocationServicesSettingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> requestLocationUpdates(locationRequest, locationCallback))
                .addOnFailureListener(err -> emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings)."));
    }

    private void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            throw e;
        }
    }
}
