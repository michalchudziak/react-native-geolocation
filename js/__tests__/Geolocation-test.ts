/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import { NativeModules } from 'react-native';
import Geolocation from '../';

jest.mock('../utils', () => {
  return {
    logError: jest.fn(),
    warning: jest.fn(),
  };
});
import { warning } from '../utils';

describe('react-native-geolocation', () => {
  afterEach(() => {
    Geolocation.stopObserving();
  });

  it('should set the location observer configuration', () => {
    Geolocation.setRNConfiguration({ skipPermissionRequests: true });
    expect(
      NativeModules.RNCGeolocation.setConfiguration.mock.calls.length
    ).toEqual(1);
  });

  it('should request authorization for location requests', () => {
    Geolocation.requestAuthorization();
    expect(
      NativeModules.RNCGeolocation.requestAuthorization.mock.calls.length
    ).toEqual(1);
  });

  it('should get the current position and pass it to the given callback', () => {
    const callback = () => {};
    Geolocation.getCurrentPosition(callback);
    expect(
      NativeModules.RNCGeolocation.getCurrentPosition.mock.calls.length
    ).toEqual(1);
    expect(
      NativeModules.RNCGeolocation.getCurrentPosition.mock.calls[0][1]
    ).toBe(callback);
  });

  it('should add a success listener to the geolocation', () => {
    const watchID = Geolocation.watchPosition(() => {});
    expect(watchID).toEqual(0);
    expect(NativeModules.RNCGeolocation.addListener.mock.calls[0][0]).toBe(
      'geolocationDidChange'
    );
  });

  it('should add an error listener to the geolocation', () => {
    const watchID = Geolocation.watchPosition(
      () => {},
      () => {}
    );
    expect(watchID).toEqual(0);
    expect(NativeModules.RNCGeolocation.addListener.mock.calls[1][0]).toBe(
      'geolocationError'
    );
  });

  it('should clear the listeners associated with a watchID', () => {
    const watchID = Geolocation.watchPosition(
      () => {},
      () => {}
    );
    Geolocation.clearWatch(watchID);
    expect(NativeModules.RNCGeolocation.stopObserving.mock.calls.length).toBe(
      1
    );
  });

  it('should correctly assess if all listeners have been cleared', () => {
    const watchID = Geolocation.watchPosition(
      () => {},
      () => {}
    );
    Geolocation.watchPosition(
      () => {},
      () => {}
    );
    Geolocation.clearWatch(watchID);
    expect(NativeModules.RNCGeolocation.stopObserving.mock.calls.length).toBe(
      0
    );
  });

  it('should not fail if the watchID one wants to clear does not exist', () => {
    Geolocation.watchPosition(
      () => {},
      () => {}
    );
    Geolocation.clearWatch(42);
    expect(NativeModules.RNCGeolocation.stopObserving.mock.calls.length).toBe(
      0
    );
  });

  it('should stop observing and warn about removing existing subscriptions', () => {
    const mockWarningCallback = jest.fn();
    (warning as jest.MockedFunction<typeof warning>).mockImplementation(
      mockWarningCallback
    );

    Geolocation.watchPosition(
      () => {},
      () => {}
    );
    Geolocation.stopObserving();
    expect(NativeModules.RNCGeolocation.stopObserving.mock.calls.length).toBe(
      1
    );
    expect(mockWarningCallback.mock.calls.length).toBeGreaterThanOrEqual(1);
  });
});
