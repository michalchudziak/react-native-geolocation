/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.geolocation;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

/**
 * @see {https://developer.mozilla.org/en-US/docs/Web/API/PositionError}
 */
public class PositionError {
  /**
   * The acquisition of the geolocation information failed because
   * the page didn't have the permission to do it.
   */
  public static int PERMISSION_DENIED = 1;

  /**
   * The acquisition of the geolocation failed because at least one
   * internal source of position returned an internal error.
   */
  public static int POSITION_UNAVAILABLE = 2;

  /**
   * The time allowed to acquire the geolocation, defined by
   * PositionOptions.timeout information was reached before the information was obtained.
   */
  public static int TIMEOUT = 3;

  /**
   * Getting the current Activity returned null, but the logic requires a non-null Activity.
   * This error can then be returned to inform the user of some underlying Android error.
   */
  public static int ACTIVITY_NULL = 4;

  public static WritableMap buildError(int code, String message) {
    WritableMap error = Arguments.createMap();
    error.putInt("code", code);

    if (message != null) {
      error.putString("message", message);
    }

    /**
    * Provide error types in error message. Feature parity with iOS
    */
    error.putInt("PERMISSION_DENIED", PERMISSION_DENIED);
    error.putInt("POSITION_UNAVAILABLE", POSITION_UNAVAILABLE);
    error.putInt("TIMEOUT", TIMEOUT);
    error.putInt("ACTIVITY_NULL", ACTIVITY_NULL);
    return error;
  }
}
