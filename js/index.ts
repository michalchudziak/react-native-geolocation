/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import * as GeolocationModule from './implementation';

import type {
  GeolocationOptions,
  GeolocationConfiguration,
  GeolocationResponse,
  GeolocationError,
} from './types';

const Geolocation = {
  /**
   * Invokes the success callback once with the latest location info.  Supported
   * options: timeout (ms), maximumAge (ms), enableHighAccuracy (bool)
   * On Android, this can return almost immediately if the location is cached or
   * request an update, which might take a while.
   */
  getCurrentPosition: function (
    success: (position: GeolocationResponse) => void,
    error?: (error: GeolocationError) => void,
    options?: GeolocationOptions
  ) {
    GeolocationModule.getCurrentPosition(success, error, options);
  },

  /**
   * Invokes the success callback whenever the location changes.  Supported
   * options: timeout (ms), maximumAge (ms), enableHighAccuracy (bool), distanceFilter(m)
   */
  watchPosition: function (
    success: (position: GeolocationResponse) => void,
    error?: (error: GeolocationError) => void,
    options?: GeolocationOptions
  ): number {
    return GeolocationModule.watchPosition(success, error, options);
  },

  clearWatch: function (watchID: number) {
    GeolocationModule.clearWatch(watchID);
  },

  stopObserving: function () {
    GeolocationModule.stopObserving();
  },

  requestAuthorization: function () {
    GeolocationModule.requestAuthorization();
  },

  setRNConfiguration: function (config: GeolocationConfiguration) {
    GeolocationModule.setRNConfiguration(config);
  },
};

export default Geolocation;
