/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow
 */

type GeoOptions = {
  timeout?: number,
  maximumAge?: number,
  enableHighAccuracy?: boolean,
};

const Geolocation = {
  setRNConfiguration: function() {
    throw new Error('method not supported by the browser');
  },
  requestAuthorization: function() {
    throw new Error('method not supported by the browser');
  },
  getCurrentPosition: async function(
    success: Function,
    error?: Function,
    options?: GeoOptions,
  ) {
    if (typeof success !== 'function') {
      throw new Error('success callback must be a function');
    } else if (!navigator || !navigator.geolocation) {
      console.error('Navigator is undefined');
      return;
    }
    navigator.geolocation.getCurrentPosition(success, error, options);
  },
  watchPosition: function(
    success: Function,
    error?: Function,
    options?: GeoOptions,
  ) {
    if (typeof success !== 'function') {
      throw new Error('success callback must be a function');
    } else if (!navigator || !navigator.geolocation) {
      console.error('Navigator is undefined');
      return;
    }
    navigator.geolocation.watchPosition(success, error, options);
  },
  clearWatch: function(watchID: number) {
    if (!navigator || !navigator.geolocation) {
      console.error('Navigator is undefined');
      return;
    }
    navigator.geolocation.clearWatch(watchID);
  },
  stopObserving: function() {
    throw new Error('method not supported by the browser');
  },
};
module.exports = Geolocation;
