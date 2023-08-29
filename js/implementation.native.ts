/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import type { EmitterSubscription } from 'react-native';

import GeolocationNativeInterface from './nativeInterface';

import invariant from 'invariant';
import { logError, warning } from './utils';

import type {
  GeolocationOptions,
  GeolocationConfiguration,
  GeolocationResponse,
  GeolocationError,
} from './NativeRNCGeolocation';

const { RNCGeolocation, GeolocationEventEmitter } = GeolocationNativeInterface;

let subscriptions: (
  | [EmitterSubscription, EmitterSubscription | null]
  | undefined
)[] = [];
let updatesEnabled = false;

/**
 * The Geolocation API extends the web spec:
 * https://developer.mozilla.org/en-US/docs/Web/API/Geolocation
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html
 */

/*
 * Sets configuration options that will be used in all location requests.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#setrnconfiguration
 *
 */
export function setRNConfiguration(config: GeolocationConfiguration) {
  RNCGeolocation.setConfiguration({
    ...config,
    enableBackgroundLocationUpdates:
      config?.enableBackgroundLocationUpdates ?? true,
    authorizationLevel:
      config?.authorizationLevel === 'auto'
        ? undefined
        : config.authorizationLevel,
    locationProvider:
      config?.locationProvider === 'auto' ? undefined : config.locationProvider,
  });
}

/*
 * Requests Location permissions based on the key configured on pList.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#requestauthorization
 */
export function requestAuthorization(
  success: () => void = () => {},
  error: (error: GeolocationError) => void = logError
) {
  RNCGeolocation.requestAuthorization(success, error);
}

/*
 * Invokes the success callback once with the latest location info.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#getcurrentposition
 */
export async function getCurrentPosition(
  success: (position: GeolocationResponse) => void,
  error: (error: GeolocationError) => void = logError,
  options: GeolocationOptions = {}
) {
  invariant(
    typeof success === 'function',
    'Must provide a valid geo_success callback.'
  );
  // Permission checks/requests are done on the native side
  RNCGeolocation.getCurrentPosition(options, success, error);
}

/*
 * Invokes the success callback whenever the location changes.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#watchposition
 */
export function watchPosition(
  success: (position: GeolocationResponse) => void,
  error: (error: GeolocationError) => void = logError,
  options: GeolocationOptions = {}
): number {
  if (!updatesEnabled) {
    RNCGeolocation.startObserving(options);
    updatesEnabled = true;
  }
  const watchID = subscriptions.length;
  subscriptions.push([
    GeolocationEventEmitter.addListener('geolocationDidChange', success),
    error
      ? GeolocationEventEmitter.addListener('geolocationError', error)
      : null,
  ]);
  return watchID;
}

/*
 * Unsubscribes the watcher with the given watchID.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#clearwatch
 */
export function clearWatch(watchID: number) {
  const sub = subscriptions[watchID];
  if (!sub) {
    // Silently exit when the watchID is invalid or already cleared
    // This is consistent with timers
    return;
  }

  sub[0].remove();
  // array element refinements not yet enabled in Flow
  const sub1 = sub[1];
  sub1 && sub1.remove();
  subscriptions[watchID] = undefined;
  let noWatchers = true;
  for (let ii = 0; ii < subscriptions.length; ii++) {
    if (subscriptions[ii]) {
      noWatchers = false; // still valid subscriptions
    }
  }
  if (noWatchers) {
    stopObserving();
  }
}

/*
 * Stops observing for device location changes and removes all registered listeners.
 *
 * See https://facebook.github.io/react-native/docs/geolocation.html#stopobserving
 */
export function stopObserving() {
  if (updatesEnabled) {
    RNCGeolocation.stopObserving();
    updatesEnabled = false;
    for (let ii = 0; ii < subscriptions.length; ii++) {
      const sub = subscriptions[ii];
      if (sub) {
        warning(false, 'Called stopObserving with existing subscriptions.');
        sub[0].remove();
        // array element refinements not yet enabled in Flow
        const sub1 = sub[1];
        sub1 && sub1.remove();
      }
    }
    subscriptions = [];
  }
}
