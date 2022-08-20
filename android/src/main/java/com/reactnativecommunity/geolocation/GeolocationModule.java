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
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.facebook.react.modules.permissions.PermissionsModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;

public class GeolocationModule extends ReactContextBaseJavaModule {

  public static final String NAME = "RNCGeolocation";
  private BaseLocationManager mLocationManager;

  public GeolocationModule(ReactApplicationContext reactContext) {
    super(reactContext);
    GoogleApiAvailability availability = new GoogleApiAvailability();
    if (availability.isGooglePlayServicesAvailable(reactContext.getApplicationContext()) == ConnectionResult.SUCCESS) {
      mLocationManager = new PlayServicesLocationManager(reactContext);
    } else {
      mLocationManager = new AndroidLocationManager(reactContext);
    }

  }

  @Override
  public String getName() {
    return NAME;
  }

  public void setConfiguration(ReadableMap config) {
    onConfigutationChange(config);
  }

  private void onConfigutationChange(ReadableMap config) {
    if (config.hasKey("locationProvider")) {
      if (config.getString("locationProvider") == "android" && mLocationManager instanceof PlayServicesLocationManager) {
        mLocationManager = new AndroidLocationManager(mLocationManager.mReactContext);
      } else if (config.getString("locationProvider") == "playServices" && mLocationManager instanceof AndroidLocationManager) {
        mLocationManager = new PlayServicesLocationManager(mLocationManager.mReactContext);
      }
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
  public void getCurrentPosition(
      final ReadableMap options,
      final Callback success,
      final Callback error) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        final PermissionsModule perms = getReactApplicationContext().getNativeModule(PermissionsModule.class);
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add( Manifest.permission.ACCESS_FINE_LOCATION);
        ReadableArray permissionsArray = JavaOnlyArray.from(permissions);

        final Callback onPermissionGranted = new Callback() {
          @Override
          public void invoke(Object... args) {
            String result = (String) args[0];
            if (result == "granted") {
              mLocationManager.getCurrentLocationData(options, success, error);
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
              perms.requestMultiplePermissions(permissionsArray, new PromiseImpl(onPermissionGranted, onPermissionDenied));
            } else {
              mLocationManager.getCurrentLocationData(options, success, error);
            }
          }
        };

        perms.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, new PromiseImpl(onPermissionChecked, onPermissionCheckFailed));
        return;
      }

      mLocationManager.getCurrentLocationData(options, success, error);
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
  public void startObserving(ReadableMap options) {
    try {
      mLocationManager.startObserving(options);
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
  public void stopObserving() {
    mLocationManager.stopObserving();
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
}
