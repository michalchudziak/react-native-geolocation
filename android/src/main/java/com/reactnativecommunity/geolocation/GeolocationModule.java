/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.geolocation;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.PromiseImpl;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.SystemClock;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.facebook.react.modules.permissions.PermissionsModule;
import javax.annotation.Nullable;

/**
 * Native module that exposes Geolocation to JS.
 */
@SuppressLint("MissingPermission")
@ReactModule(name = GeolocationModule.NAME)
public class GeolocationModule extends ReactContextBaseJavaModule {

  public static final String NAME = "RNCGeolocation";
  private @Nullable String mWatchedProvider;
  private static final float RCT_DEFAULT_LOCATION_ACCURACY = 100;

  private final LocationListener mLocationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {
      getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
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

  public GeolocationModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return NAME;
  }

  private static class LocationOptions {
    private final long timeout;
    private final double maximumAge;
    private final boolean highAccuracy;
    private final float distanceFilter;

    private LocationOptions(
      long timeout,
      double maximumAge,
      boolean highAccuracy,
      float distanceFilter) {
      this.timeout = timeout;
      this.maximumAge = maximumAge;
      this.highAccuracy = highAccuracy;
      this.distanceFilter = distanceFilter;
    }

    private static LocationOptions fromReactMap(ReadableMap map) {
      // precision might be dropped on timeout (double -> int conversion), but that's OK
      long timeout =
          map.hasKey("timeout") ? (long) map.getDouble("timeout") : Long.MAX_VALUE;
      double maximumAge =
          map.hasKey("maximumAge") ? map.getDouble("maximumAge") : Double.POSITIVE_INFINITY;
      boolean highAccuracy =
          map.hasKey("enableHighAccuracy") && map.getBoolean("enableHighAccuracy");
      float distanceFilter = map.hasKey("distanceFilter") ?
        (float) map.getDouble("distanceFilter") :
        RCT_DEFAULT_LOCATION_ACCURACY;

      return new LocationOptions(timeout, maximumAge, highAccuracy, distanceFilter);
    }
  }

  /**
   * Get the current position. This can return almost immediately if the location is cached or
   * request an update, which might take a while. This method also requests location
   * permissions on API level 23 and above when needed.
   *
   * @param options map containing optional arguments: timeout (millis), maximumAge (millis) and
   *        highAccuracy (boolean)
   */
  @ReactMethod
  public void getCurrentPosition(
      final ReadableMap options,
      final Callback success,
      final Callback error) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final PermissionsModule perms = getReactApplicationContext().getNativeModule(PermissionsModule.class);

      final Callback onPermissionGranted = new Callback() {
        @Override
        public void invoke(Object... args) {
          String result = (String) args[0];
          if (result == "granted") {
            getCurrentLocationData(options, success, error);
          } else {
            error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Location permission was not granted."));
          }
        }
      };

      final Callback onPermissionDenied = new Callback() {
        @Override
        public void invoke(Object... args) {
          error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Failed to request location permission."));
        }
      };

      Callback onPermissionCheckFailed = new Callback() {
        @Override
        public void invoke(Object... args) {
          error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Failed to check location permission."));
        }
      };

      Callback onPermissionChecked = new Callback() {
        @Override
        public void invoke(Object... args) {
          boolean hasPermission = (boolean) args[0];

          if (!hasPermission) {
            perms.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, new PromiseImpl(onPermissionGranted, onPermissionDenied));
          } else {
            getCurrentLocationData(options, success, error);
          }
        }
      };

      perms.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, new PromiseImpl(onPermissionChecked, onPermissionCheckFailed));
      return;
    }

    getCurrentLocationData(options, success, error);
  }

  /**
   * Get the current position. This can return almost immediately if the location is cached or
   * request an update, which might take a while.
   *
   * @param options map containing optional arguments: timeout (millis), maximumAge (millis) and
   *        highAccuracy (boolean)
   */
  public void getCurrentLocationData(
      ReadableMap options,
      final Callback success,
      Callback error) {
    LocationOptions locationOptions = LocationOptions.fromReactMap(options);

    try {
      LocationManager locationManager =
          (LocationManager) getReactApplicationContext().getSystemService(Context.LOCATION_SERVICE);
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

      new SingleUpdateRequest(locationManager, provider, locationOptions.timeout, success, error)
          .invoke(location);
    } catch (SecurityException e) {
      throwLocationPermissionMissing(e);
    }
  }

  /**
   * Start listening for location updates. These will be emitted via the
   * {@link RCTDeviceEventEmitter} as {@code geolocationDidChange} events.
   *
   * @param options map containing optional arguments: highAccuracy (boolean)
   */
  @ReactMethod
  public void startObserving(ReadableMap options) {
    if (LocationManager.GPS_PROVIDER.equals(mWatchedProvider)) {
      return;
    }
    LocationOptions locationOptions = LocationOptions.fromReactMap(options);

    try {
      LocationManager locationManager =
          (LocationManager) getReactApplicationContext().getSystemService(Context.LOCATION_SERVICE);
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
      throwLocationPermissionMissing(e);
    }
  }

  /**
   * Stop listening for location updates.
   *
   * NB: this is not balanced with {@link #startObserving}: any number of calls to that method will
   * be canceled by just one call to this one.
   */
  @ReactMethod
  public void stopObserving() {
    LocationManager locationManager =
        (LocationManager) getReactApplicationContext().getSystemService(Context.LOCATION_SERVICE);
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
    int finePermission = ContextCompat.checkSelfPermission(getReactApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
    if (provider.equals(LocationManager.GPS_PROVIDER) && finePermission != PackageManager.PERMISSION_GRANTED) {
      return null;
    }
    return provider;
  }

  private static WritableMap locationToMap(Location location) {
    WritableMap map = Arguments.createMap();
    WritableMap coords = Arguments.createMap();
    coords.putDouble("latitude", location.getLatitude());
    coords.putDouble("longitude", location.getLongitude());
    coords.putDouble("altitude", location.getAltitude());
    coords.putDouble("accuracy", location.getAccuracy());
    coords.putDouble("heading", location.getBearing());
    coords.putDouble("speed", location.getSpeed());
    map.putMap("coords", coords);
    map.putDouble("timestamp", location.getTime());

    Bundle bundle = location.getExtras();
    if (bundle != null) {
      WritableMap extras = Arguments.createMap();
      for (String key: bundle.keySet()) {
        putIntoMap(extras, key, bundle.get(key));
      }

      map.putMap("extras", extras);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      map.putBoolean("mocked", location.isFromMockProvider());
    }

    return map;
  }

  private static void putIntoMap(WritableMap map, String key, Object value) {
    if (value instanceof Integer || value instanceof Long) {
      map.putInt(key, (Integer) value);
    } else if (value instanceof Float) {
      map.putDouble(key, (Float) value);
    } else if (value instanceof Double) {
      map.putDouble(key, (Double) value);
    } else if (value instanceof String) {
      map.putString(key, (String) value);
    } else if (value instanceof Boolean) {
      map.putBoolean(key, (Boolean) value);
    } else if (value instanceof int[]
            || value instanceof long[]
            || value instanceof double[]
            || value instanceof String[]
            || value instanceof boolean[]) {
      map.putArray(key,  Arguments.fromArray(value));
    }
  }

  private void emitError(int code, String message) {
    getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
        .emit("geolocationError", PositionError.buildError(code, message));
  }

  /**
   * Provides a clearer exception message than the default one.
   */
  private static void throwLocationPermissionMissing(SecurityException e) {
    throw new SecurityException(
      "Looks like the app doesn't have the permission to access location.\n" +
      "Add the following line to your app's AndroidManifest.xml:\n" +
      "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />", e);
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
