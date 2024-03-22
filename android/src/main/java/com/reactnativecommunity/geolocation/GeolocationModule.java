/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.geolocation;

import android.Manifest;
import android.os.Build;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.PromiseImpl;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.facebook.react.modules.permissions.PermissionsModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class GeolocationModule extends ReactContextBaseJavaModule {

  public static final String NAME = "RNCGeolocation";
  private BaseLocationManager mLocationManager;
  private Configuration mConfiguration;

  public GeolocationModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mConfiguration = Configuration.getDefault();
    mLocationManager = new AndroidLocationManager(reactContext);
  }

  @Override
  public String getName() {
    return NAME;
  }

  public void setConfiguration(ReadableMap config) {
    mConfiguration = Configuration.fromReactMap(config);
    onConfigurationChange(mConfiguration);
  }

  private void onConfigurationChange(Configuration config) {
    ReactApplicationContext reactContext = mLocationManager.mReactContext;
    if (Objects.equals(config.locationProvider, "android") && mLocationManager instanceof PlayServicesLocationManager) {
      mLocationManager = new AndroidLocationManager(reactContext);
    } else if (Objects.equals(config.locationProvider, "playServices") && mLocationManager instanceof AndroidLocationManager) {
      GoogleApiAvailability availability = new GoogleApiAvailability();
      if (availability.isGooglePlayServicesAvailable(reactContext.getApplicationContext()) == ConnectionResult.SUCCESS) {
        mLocationManager = new PlayServicesLocationManager(reactContext);
      }
    }
  }

  /**
   * Requests location permission.
   */
  public void requestAuthorization(final Callback success, final Callback error) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final PermissionsModule perms = getReactApplicationContext().getNativeModule(PermissionsModule.class);
      ArrayList<String> permissions = new ArrayList<>();
      permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
      permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
      ReadableArray permissionsArray = JavaOnlyArray.from(permissions);

      final Callback onPermissionGranted = args -> {
        WritableNativeMap result = (WritableNativeMap) args[0];
        if (result.getString(Manifest.permission.ACCESS_COARSE_LOCATION).equals("granted")) {
          success.invoke();
        } else {
          error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Location permission was not granted."));
        }
      };

      final Callback onPermissionDenied = args -> error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Failed to request location permission."));

      Callback onPermissionCheckFailed = args -> error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED, "Failed to check location permission."));

      Callback onPermissionChecked = args -> {
        boolean hasPermission = (boolean) args[0];

        if (!hasPermission) {
          perms.requestMultiplePermissions(permissionsArray, new PromiseImpl(onPermissionGranted, onPermissionDenied));
        } else {
          success.invoke();
        }
      };

      perms.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, new PromiseImpl(onPermissionChecked, args -> perms.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, new PromiseImpl(onPermissionChecked, onPermissionCheckFailed))));
      return;
    }

    success.invoke();
  }

  /**
   * Get the current position. This can return almost immediately if the location is cached or
   * request an update, which might take a while. This method also requests location
   * permissions on API level 23 and above when needed.
   *
   * @param options map containing optional arguments: timeout (millis), maximumAge (millis) and
   *        highAccuracy (boolean)
   */
  public void getCurrentPosition(
      final ReadableMap options,
      final Callback success,
      final Callback error) {
    try {
      if (mConfiguration.skipPermissionRequests) {
        mLocationManager.getCurrentLocationData(options, success, error);
        return;
      }

      requestAuthorization(args -> mLocationManager.getCurrentLocationData(options, success, error), error);
    } catch (SecurityException e) {
      emitLocationPermissionMissing(e);
    }
  }


  /**
   * Start listening for location updates. These will be emitted via the
   * {@link RCTDeviceEventEmitter} as {@code geolocationDidChange} events.
   *
   * @param options map containing optional arguments: highAccuracy (boolean)
   */
  public void startObserving(ReadableMap options) {
    try {
      if (mConfiguration.skipPermissionRequests) {
        mLocationManager.startObserving(options);
        return;
      }

      requestAuthorization(args -> mLocationManager.startObserving(options), args -> {
        emitLocationPermissionMissing(new SecurityException(Arrays.toString(args)));
      });
    } catch (SecurityException e) {
      emitLocationPermissionMissing(e);
    }
  }

  /**
   * Stop listening for location updates.
   *
   * NB: this is not balanced with {@link #startObserving}: any number of calls to that method will
   * be canceled by just one call to this one.
   */
  public void stopObserving() {
    mLocationManager.stopObserving();
  }

  /**
   * Provides a clearer exception message than the default one.
   */
  private void emitLocationPermissionMissing(SecurityException e) {
    String message =
            "Looks like the app doesn't have the permission to access location.\n" +
                    "Add the following line to your app's AndroidManifest.xml:\n" +
                    "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n" +
                    e.getMessage();
    mLocationManager.emitError(PositionError.PERMISSION_DENIED, message);
  }

  private static class Configuration {
    String locationProvider;
    Boolean skipPermissionRequests;

    private Configuration(String locationProvider, boolean skipPermissionRequests) {
      this.locationProvider = locationProvider;
      this.skipPermissionRequests = skipPermissionRequests;
    }

    protected static Configuration getDefault() {
      return new Configuration("auto", false);
    }

    protected static Configuration fromReactMap(ReadableMap map) {
      String locationProvider =
              map.hasKey("locationProvider") ? map.getString("locationProvider") : "auto";
      boolean skipPermissionRequests =
              map.hasKey("skipPermissionRequests") ? map.getBoolean("skipPermissionRequests") : false;
      return new Configuration(locationProvider, skipPermissionRequests);
    }
  }
}
