package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.content.Context;
import android.location.LocationManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

@SuppressLint("MissingPermission")
public class PlayServicesLocationManager extends BaseLocationManager {
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private SettingsClient mLocationServicesSettingsClient;

    protected PlayServicesLocationManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext);
        mLocationServicesSettingsClient = LocationServices.getSettingsClient(reactContext);
    }

    @Override
    public void getCurrentLocationData(ReadableMap options, Callback success, Callback error) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                        success.invoke(locationToMap(location));
                    } else {
                        fetchCurrentLocation(locationOptions, success, error);
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof SecurityException) {
                        error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Location permission was not granted (FusedLocationProvider/lastLocation/denied)."));
                        return;
                    }
                    fetchCurrentLocation(locationOptions, success, error);
                })
                .addOnCanceledListener(() -> fetchCurrentLocation(locationOptions, success, error));
    }

    private void fetchCurrentLocation(LocationOptions options, Callback success, Callback error) {
        if (options.timeout <= 0) {
            error.invoke(PositionError.buildError(PositionError.TIMEOUT, "Location request timed out"));
            return;
        }
        mFusedLocationClient.getCurrentLocation(buildCurrentLocationRequest(options), null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        if (!isAnyProviderAvailable()) {
                            error.invoke(PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings)."));
                            return;
                        }
                        error.invoke(PositionError.buildError(PositionError.TIMEOUT, "No location provided (FusedLocationProvider/currentLocation/null)."));
                        return;
                    }
                    success.invoke(locationToMap(location));
                })
                .addOnFailureListener(e -> {
                    if (e instanceof SecurityException) {
                        error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Location permission was not granted (FusedLocationProvider/currentLocation/denied)."));
                        return;
                    }
                    error.invoke(PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/currentLocation/failure)."));
                })
                .addOnCanceledListener(() -> error.invoke(PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location request cancelled (FusedLocationProvider/currentLocation/canceled).")));
    }

    private CurrentLocationRequest buildCurrentLocationRequest(LocationOptions options) {
        return new CurrentLocationRequest.Builder()
                .setPriority(getPriority(options.highAccuracy))
                .setMaxUpdateAgeMillis((long) options.maximumAge)
                .setDurationMillis(options.timeout)
                .build();
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

        checkLocationSettings(options, mLocationCallback, null);
    }

    @Override
    public void stopObserving() {
        if(mLocationCallback == null) {
            return;
        }
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void checkLocationSettings(ReadableMap options, LocationCallback locationCallback, Callback error) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(locationOptions.interval);
        requestBuilder.setPriority(getPriority(locationOptions.highAccuracy));
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
                .addOnFailureListener(err -> {
                    if(isAnyProviderAvailable()){
                        requestLocationUpdates(locationRequest, locationCallback);
                        return;
                    }

                    if (error != null) {
                        error.invoke(
                            PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings).")
                        );
                        return;
                    }
                    emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings).");
                });
    }

    private static int getPriority(boolean highAccuracy) {
        return highAccuracy ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_LOW_POWER;
    }

    private void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            throw e;
        }
    }

    private boolean isAnyProviderAvailable() {
        if (mReactContext == null) {
        return false;
        }
        LocationManager locationManager =
                    (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
}
