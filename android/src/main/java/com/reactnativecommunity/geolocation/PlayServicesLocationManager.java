package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
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

import javax.annotation.Nullable;

@SuppressLint("MissingPermission")
public class PlayServicesLocationManager extends BaseLocationManager {
    private boolean mIsServiceRunning = false;
    private @Nullable LocationHandler mLocationHandler;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mLocationHandler = binder.getService();
            final PlayServicesLocationManager.LocationHandlerImpl locationHandlerImpl =
                    new PlayServicesLocationManager.LocationHandlerImpl(
                            (LocationService) mLocationHandler,
                            mReactContext.getCurrentActivity(),
                            PlayServicesLocationManager.this);
            ((LocationService) mLocationHandler).setLocationHandler(locationHandlerImpl);
            mIsServiceRunning = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLocationHandler = null;
            mIsServiceRunning = false;
        }
    };

    protected PlayServicesLocationManager(ReactApplicationContext reactContext, boolean enableBackgroundLocationUpdates) {
        super(reactContext, enableBackgroundLocationUpdates);
        Log.d("PlayServicesLocationManager", "enableBackgroundLocationUpdates:" + enableBackgroundLocationUpdates);

        if (enableBackgroundLocationUpdates) {
            startService();
        } else {
            mLocationHandler = new LocationHandlerImpl(reactContext, mReactContext.getCurrentActivity(), this);
        }
    }

    @Override
    public void getCurrentLocationData(ReadableMap options, Callback success, Callback error) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);

        try {
            if (mIsServiceRunning) {
                if (!mEnableBackgroundLocationUpdates) {
                    stopService();
                    mLocationHandler = new LocationHandlerImpl(mReactContext, mReactContext.getCurrentActivity(), this);
                } else if (mLocationHandler == null) {
                    mLocationHandler = new LocationHandlerImpl(mReactContext, mReactContext.getCurrentActivity(), this);
                }
            } else {
                if (mEnableBackgroundLocationUpdates) {
                    startService();
                }
            }

            mLocationHandler.getCurrentLocation(locationOptions, success, error);
        } catch (SecurityException e) {
            throw e;
        }
    }

    @Override
    public void startObserving(ReadableMap options) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        mLocationHandler.startLocationUpdates(locationOptions);
    }

    @Override
    public void stopObserving() {
        mLocationHandler.stopLocationUpdates();
    }

    private void startService() {
        if (mIsServiceRunning) {
            return;
        }

        Intent intent = new Intent(mReactContext, LocationService.class);

        // Start the LocationService as a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mReactContext.startForegroundService(intent);
        } else {
            mReactContext.startService(intent);
        }

        mReactContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void stopService() {
        if (!mIsServiceRunning) {
            return;
        }

        mReactContext.unbindService(mConnection);
        Intent intent = new Intent(mReactContext, LocationService.class);
        mReactContext.stopService(intent);
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

    private static class LocationHandlerImpl implements LocationHandler {
        private static final String TAG = "PlayServicesLocationHandlerImpl";
        private final Context mContext;
        private final Activity mActivity;
        private FusedLocationProviderClient mFusedLocationClient;
        private SettingsClient mLocationServicesSettingsClient;
        private LocationCallback mSingleLocationCallback;
        private LocationCallback mLocationCallback;
        private final EventEmitter mEventEmitter;

        public LocationHandlerImpl(Context context, Activity activity, EventEmitter errorEmitter) {
            mContext = context;
            mActivity = activity;
            mEventEmitter = errorEmitter;
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            mLocationServicesSettingsClient = LocationServices.getSettingsClient(context);
        }

        public void startLocationUpdates(LocationOptions options) {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        mEventEmitter.emitError(PositionError.POSITION_UNAVAILABLE, "No location provided (FusedLocationProvider/observer).");
                        return;
                    }

                    mEventEmitter.emit("geolocationDidChange", locationToMap(locationResult.getLastLocation()));
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    if (!locationAvailability.isLocationAvailable()) {
                        mEventEmitter.emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider).");
                    }
                }
            };

            checkLocationSettings(options, mLocationCallback, null);
        }

        public void stopLocationUpdates() {
            if (mLocationCallback == null) {
                return;
            }
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        public void getCurrentLocation(LocationOptions locationOptions, final Callback success,
                                       Callback error) {
            if (mActivity == null) {
                mSingleLocationCallback = createSingleLocationCallback(success, error);
                checkLocationSettings(locationOptions, mSingleLocationCallback, error);
                return;
            }

            try {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(mActivity, location -> {
                            if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                                success.invoke(locationToMap(location));
                            } else {
                                mSingleLocationCallback = createSingleLocationCallback(success, error);
                                checkLocationSettings(locationOptions, mSingleLocationCallback, error);
                            }
                        });
            } catch (SecurityException e) {
                throw e;
            }
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

        private void requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
            try {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                throw e;
            }
        }

        private void checkLocationSettings(LocationOptions locationOptions, LocationCallback locationCallback, Callback error) {
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
                        if (isAnyProviderAvailable()) {
                            requestLocationUpdates(locationRequest, locationCallback);
                            return;
                        }

                        if (error != null) {
                            error.invoke(
                                    PositionError.buildError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings).")
                            );
                            return;
                        }
                        mEventEmitter.emitError(PositionError.POSITION_UNAVAILABLE, "Location not available (FusedLocationProvider/settings).");
                    });
        }

        private boolean isAnyProviderAvailable() {
            if (mContext == null) {
                return false;
            }
            LocationManager locationManager =
                    (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        }
    }
}
