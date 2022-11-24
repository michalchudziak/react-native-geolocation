package com.reactnativecommunity.geolocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import javax.annotation.Nullable;

@SuppressLint("MissingPermission")
public class AndroidLocationManager extends BaseLocationManager {
    private @Nullable
    String mWatchedProvider;

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
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    };

    protected AndroidLocationManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    public void getCurrentLocationData(
            ReadableMap options,
            final Callback success,
            Callback error) {
        AndroidLocationManager.LocationOptions locationOptions = AndroidLocationManager.LocationOptions.fromReactMap(options);

        try {
            LocationManager locationManager =
                    (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
            String provider = getValidProvider(locationManager, locationOptions.highAccuracy);
            if (provider == null) {
                error.invoke(
                        PositionError.buildError(
                                PositionError.POSITION_UNAVAILABLE, "No location provider available."));
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
                success.invoke(locationToMap(location));
                return;
            }

            new AndroidLocationManager.SingleUpdateRequest(locationManager, provider, locationOptions.timeout, success, error)
                    .invoke(location);
        } catch (SecurityException e) {
            throw e;
        }
    }

    public void startObserving(ReadableMap options) {
        if (LocationManager.GPS_PROVIDER.equals(mWatchedProvider)) {
            return;
        }
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);

        try {
            LocationManager locationManager =
                    (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
            String provider = getValidProvider(locationManager, locationOptions.highAccuracy);
            if (provider == null) {
                emitError(PositionError.POSITION_UNAVAILABLE, "No location provider available.");
                return;
            }
            if (!provider.equals(mWatchedProvider)) {
                locationManager.removeUpdates(mLocationListener);
                locationManager.requestLocationUpdates(
                        provider,
                        1000,
                        locationOptions.distanceFilter,
                        mLocationListener);
            }
            mWatchedProvider = provider;
        } catch (SecurityException e) {
            throw e;
        }
    }

    public void stopObserving() {
        LocationManager locationManager =
                (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(mLocationListener);
        mWatchedProvider = null;
    }

    @Nullable
    private String getValidProvider(LocationManager locationManager, boolean highAccuracy) {
        String provider =
                highAccuracy ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        if (!locationManager.isProviderEnabled(provider)) {
            provider = provider.equals(LocationManager.GPS_PROVIDER)
                    ? LocationManager.NETWORK_PROVIDER
                    : LocationManager.GPS_PROVIDER;
            if (!locationManager.isProviderEnabled(provider)) {
                return null;
            }
        }
        // If it's an enabled provider, but we don't have permissions, ignore it
        int finePermission = ContextCompat.checkSelfPermission(mReactContext, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ContextCompat.checkSelfPermission(mReactContext, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (provider.equals(LocationManager.GPS_PROVIDER) && (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED)) {
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
                        mError.invoke(PositionError.buildError(PositionError.TIMEOUT, "Location request timed out"));
                        mLocationManager.removeUpdates(mLocationListener);
                        FLog.i(ReactConstants.TAG, "LocationModule: Location request timed out");
                        mTriggered = true;
                    }
                }
            }
        };
        private final LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (SingleUpdateRequest.this) {
                    if (!mTriggered && isBetterLocation(location, mOldLocation)) {
                        mSuccess.invoke(locationToMap(location));
                        mHandler.removeCallbacks(mTimeoutRunnable);
                        mTriggered = true;
                        mLocationManager.removeUpdates(mLocationListener);
                    }

                    mOldLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
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

        /** Determines whether one Location reading is better than the current Location fix
         * taken from Android Examples https://developer.android.com/guide/topics/location/strategies.html
         *
         * @param location  The new Location that you want to evaluate
         * @param currentBestLocation  The current Location fix, to which you want to compare the new one
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

        /** Checks whether two providers are the same */
        private boolean isSameProvider(String provider1, String provider2) {
            if (provider1 == null) {
                return provider2 == null;
            }
            return provider1.equals(provider2);
        }
    }
}
