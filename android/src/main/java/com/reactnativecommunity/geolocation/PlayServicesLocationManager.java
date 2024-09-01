package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;
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
            mSingleLocationCallback = createSingleLocationCallback(success, error);
            checkLocationSettings(options, mSingleLocationCallback, error);
			return;
        }

        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(currentActivity, location -> {
                        if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                            success.invoke(locationToMap(location));
                        } else {
                            mSingleLocationCallback = createSingleLocationCallback(success, error);
                            checkLocationSettings(options, mSingleLocationCallback, error);
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

    private LocationCallback createSingleLocationCallback(Callback success, Callback error) {
        final CallbackHolder callbackHolder = new CallbackHolder(success, error);

        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                if (location == null) {
                    callbackHolder.error(PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "No location provided (FusedLocationProvider/lastLocation)."));
                    return;
                }

                callbackHolder.success(location);

                mFusedLocationClient.removeLocationUpdates(mSingleLocationCallback);
                mSingleLocationCallback = null;
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    callbackHolder.error(PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/lastLocation)."));
                }
            }
        };
    }

    private static class CallbackHolder {
        Callback success;
        Callback error;
        public CallbackHolder(Callback success, Callback error) {
            this.success = success;
            this.error = error;
        }

        public void error(WritableMap cause) {
            if (this.error == null) {
                Log.e(this.getClass().getSimpleName(), "tried to invoke null error callback -> " + cause.toString());
                return;
            }
            this.error.invoke(cause);
            this.error = null;
        }

        public void success(Location location) {
            if (this.success == null) {
                Log.e(this.getClass().getSimpleName(), "tried to invoke null success callback");
                return;
            }
            this.success.invoke(locationToMap(location));
            this.success = null;
        }
    }
}
