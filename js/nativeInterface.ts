/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@react-native-community/geolocation' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const RNCGeolocationModule = isTurboModuleEnabled
  ? require('./NativeRNCGeolocation').default
  : NativeModules.RNCGeolocation;

const RNCGeolocation = RNCGeolocationModule
  ? RNCGeolocationModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * We export the native interface in this way to give easy shared access to it between the
 * JavaScript code and the tests
 */
let nativeEventEmitter: NativeEventEmitter | null = null;

export default {
  RNCGeolocation,
  get GeolocationEventEmitter() {
    if (!nativeEventEmitter) {
      nativeEventEmitter = new NativeEventEmitter(RNCGeolocation);
    }
    return nativeEventEmitter;
  },
};
