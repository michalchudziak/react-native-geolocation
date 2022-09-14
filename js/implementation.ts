/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import type {
  GeolocationOptions,
  GeolocationConfiguration,
  GeolocationResponse,
  GeolocationError,
} from './NativeRNCGeolocation';

export function setRNConfiguration(_config: GeolocationConfiguration) {
  throw new Error('setRNConfiguration is not supported by the browser');
}

export function requestAuthorization(
  _success?: () => void,
  _error?: (error: GeolocationError) => void
) {
  throw new Error('requestAuthorization is not supported by the browser');
}

export async function getCurrentPosition(
  success: (position: GeolocationResponse) => void,
  error?: (error: GeolocationError) => void,
  options?: GeolocationOptions
) {
  if (typeof success !== 'function') {
    throw new Error('success callback must be a function');
  } else if (!navigator || !navigator.geolocation) {
    console.error('Navigator is undefined');
    return;
  }
  navigator.geolocation.getCurrentPosition(success, error, options);
}

export function watchPosition(
  success: (position: GeolocationResponse) => void,
  error?: (error: GeolocationError) => void,
  options?: GeolocationOptions
): number {
  if (typeof success !== 'function') {
    throw new Error('success callback must be a function');
  } else if (!navigator || !navigator.geolocation) {
    throw new Error('Navigator is undefined');
  }
  return navigator.geolocation.watchPosition(success, error, options);
}

export function clearWatch(watchID: number) {
  if (!navigator || !navigator.geolocation) {
    console.error('Navigator is undefined');
    return;
  }
  navigator.geolocation.clearWatch(watchID);
}

export function stopObserving() {
  throw new Error('stopObserving is not supported by the browser');
}
