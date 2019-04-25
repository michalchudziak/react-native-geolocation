/*
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
    console.info('method not supported by the browser');
  },
  requestAuthorization: function() {
    console.info('method not supported by the browser');
  },
  getCurrentPosition: async function(
    success: Function,
    error?: Function,
    options?: GeoOptions,
  ) {
    if (typeof success !== 'function') {
      console.error('Success callback must be a function');
      return;
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
      console.error('Success callback must be a function');
      return;
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
    console.info('method not supported by the browser');
  },
};
module.exports = Geolocation;
