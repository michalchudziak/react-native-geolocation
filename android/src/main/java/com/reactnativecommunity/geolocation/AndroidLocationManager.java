package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.ComponentName;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;

@SuppressLint("MissingPermission")
public class AndroidLocationManager extends BaseLocationManager {
    private boolean mIsServiceRunning = false;

    private @Nullable LocationHandler mLocationHandler;
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("geolocationDidChange", locationToMap(location));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                emitError(PositionError.POSITION_UNAVAILABLE, "Provider " + provider + " is out of service.");
            } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                emitError(PositionError.TIMEOUT, "Provider " + provider + " is temporarily unavailable.");
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mLocationHandler = binder.getService();
            final AndroidLocationManager.LocationHandlerImpl locationHandlerImpl =
                    new AndroidLocationManager.LocationHandlerImpl(
                            (LocationService)mLocationHandler,
                            mLocationListener,
                            AndroidLocationManager.this);
            ((LocationService) mLocationHandler).setLocationHandler(locationHandlerImpl);
            mIsServiceRunning = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLocationHandler = null;
            mIsServiceRunning = false;
        }
    };

    protected AndroidLocationManager(ReactApplicationContext reactContext, boolean enableBackgroundLocationUpdates) {
        super(reactContext, enableBackgroundLocationUpdates);
        Log.d("AndroidLocationManager", "enableBackgroundLocationUpdates:" + enableBackgroundLocationUpdates);
        if (enableBackgroundLocationUpdates) {
            startService();
        } else {
            mLocationHandler = new LocationHandlerImpl(reactContext, mLocationListener, this);
        }
    }

    public void getCurrentLocationData(
            ReadableMap options,
            final Callback success,
            Callback error) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);

        try {
            if (mIsServiceRunning) {
                if (!mEnableBackgroundLocationUpdates) {
                    stopService();
                    mLocationHandler = new LocationHandlerImpl(mReactContext, mLocationListener, this);
                } else if (mLocationHandler == null) {
                    mLocationHandler = new LocationHandlerImpl(mReactContext, mLocationListener, this);
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
//        LocationManager locationManager =
//                (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
//        locationManager.removeUpdates(mLocationListener);
//        mWatchedProvider = null;
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

    private static class LocationHandlerImpl implements LocationHandler {
        private static final String TAG = "AndroidLocationHandlerImpl";
        private final LocationListener mLocationListener;
        private final Context mContext;
        private @Nullable String mWatchedProvider;
        private final EventEmitter mEventEmitter;

        public LocationHandlerImpl(Context context, LocationListener locationListener, EventEmitter errorEmitter) {
            mContext = context;
            mLocationListener = locationListener;
            mEventEmitter = errorEmitter;
        }

        public void startLocationUpdates(LocationOptions options) {
            Log.i(TAG, "startLocationUpdates");
            if (LocationManager.GPS_PROVIDER.equals(mWatchedProvider)) {
                return;
            }

            try {
                LocationManager locationManager =
                        (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                String provider = getValidProvider(locationManager, options.highAccuracy);
                if (provider == null) {
                    // handle error
                    // emitError(PositionError.POSITION_UNAVAILABLE, "No location provider available.");
                    mEventEmitter.emitError(PositionError.POSITION_UNAVAILABLE, "No location provider available.");
                    return;
                }
                if (!provider.equals(mWatchedProvider)) {
                    locationManager.removeUpdates(mLocationListener);
                    locationManager.requestLocationUpdates(
                            provider,
                            1000,
                            options.distanceFilter,
                            mLocationListener);
                }
                mWatchedProvider = provider;
            } catch (SecurityException e) {
                throw e;
            }
        }

        public void stopLocationUpdates() {
            Log.i(TAG, "stopLocationUpdates");
            LocationManager locationManager =
                    (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationListener);
            mWatchedProvider = null;
        }

        public void getCurrentLocation(LocationOptions options,
                                       final Callback success,
                                       Callback error) {
            Log.i(TAG, "getCurrentLocation");

            try {
                LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

                String provider = getValidProvider(locationManager, options.highAccuracy);
                if (provider == null) {
                    error.invoke(
                            PositionError.buildError(
                                    PositionError.POSITION_UNAVAILABLE, "No location provider available."));
                    return;
                }

                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < options.maximumAge) {
                    success.invoke(locationToMap(location));
                    return;
                }

                new LocationHandlerImpl.SingleUpdateRequest(locationManager, provider, options.timeout, success, error).invoke(location);
            } catch (SecurityException e) {
                throw e;
            }
        }

        @Nullable
        private String getValidProvider(LocationManager locationManager, boolean highAccuracy) {
            String provider = highAccuracy ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            if (!locationManager.isProviderEnabled(provider)) {
                provider = provider.equals(LocationManager.GPS_PROVIDER)
                        ? LocationManager.NETWORK_PROVIDER
                        : LocationManager.GPS_PROVIDER;

                if (!locationManager.isProviderEnabled(provider)) {
                    return null;
                }
            }

            // If it's an enabled provider, but we don't have permissions, ignore it
            int finePermission = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION);
            int coarsePermission = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION);

            if ((provider.equals(LocationManager.GPS_PROVIDER) && finePermission != PackageManager.PERMISSION_GRANTED) ||
                    (provider.equals(LocationManager.NETWORK_PROVIDER) && coarsePermission != PackageManager.PERMISSION_GRANTED)) {
                return null;
            }

            return provider;
        }

        private static class SingleUpdateRequest {
            private final Callback mSuccess;
            private final Callback mError;
            private final LocationManager mLocationManager;
            private final String mProvider;
            private final long mTimeout;
            private Location mOldLocation;
            private final Handler mHandler = new Handler();
            private final Runnable mTimeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (SingleUpdateRequest.this) {
                        if (!mTriggered) {
                            mError.invoke(PositionError.TIMEOUT, "Location request timed out");
                            mLocationManager.removeUpdates(mLocationListener);
                            mTriggered = true;
                        }
                    }
                }
            };
            private final LocationListener mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    synchronized (SingleUpdateRequest.this) {
                        if (!mTriggered && isBetterLocation(location, mOldLocation)) {
                            mSuccess.invoke(location);
                            mHandler.removeCallbacks(mTimeoutRunnable);
                            mTriggered = true;
                            mLocationManager.removeUpdates(mLocationListener);
                        }

                        mOldLocation = location;
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
            private boolean mTriggered;

            private SingleUpdateRequest(
                    LocationManager locationManager,
                    String provider,
                    long timeout,
                    Callback success,
                    Callback error) {
                mLocationManager = locationManager;
                mProvider = provider;
                mTimeout = timeout;
                mSuccess = success;
                mError = error;
            }

            public void invoke(Location location) {
                mOldLocation = location;
                mLocationManager.requestLocationUpdates(mProvider, 100, 1, mLocationListener);
                mHandler.postDelayed(mTimeoutRunnable, mTimeout);
            }

            private static final int TWO_MINUTES = 1000 * 60 * 2;

            /**
             * Determines whether one Location reading is better than the current Location fix
             * taken from Android Examples https://developer.android.com/guide/topics/location/strategies.html
             *
             * @param location            The new Location that you want to evaluate
             * @param currentBestLocation The current Location fix, to which you want to compare the new one
             */
            private boolean isBetterLocation(Location location, Location currentBestLocation) {
                if (currentBestLocation == null) {
                    // A new location is always better than no location
                    return true;
                }

                // Check whether the new location fix is newer or older
                long timeDelta = location.getTime() - currentBestLocation.getTime();
                boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
                boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
                boolean isNewer = timeDelta > 0;

                // If it's been more than two minutes since the current location, use the new location
                // because the user has likely moved
                if (isSignificantlyNewer) {
                    return true;
                    // If the new location is more than two minutes older, it must be worse
                } else if (isSignificantlyOlder) {
                    return false;
                }

                // Check whether the new location fix is more or less accurate
                int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
                boolean isLessAccurate = accuracyDelta > 0;
                boolean isMoreAccurate = accuracyDelta < 0;
                boolean isSignificantlyLessAccurate = accuracyDelta > 200;

                // Check if the old and new location are from the same provider
                boolean isFromSameProvider = isSameProvider(location.getProvider(),
                        currentBestLocation.getProvider());

                // Determine location quality using a combination of timeliness and accuracy
                if (isMoreAccurate) {
                    return true;
                } else if (isNewer && !isLessAccurate) {
                    return true;
                } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                    return true;
                }

                return false;
            }

            /**
             * Checks whether two providers are the same
             */
            private boolean isSameProvider(String provider1, String provider2) {
                if (provider1 == null) {
                    return provider2 == null;
                }
                return provider1.equals(provider2);
            }
        }
    }
}
