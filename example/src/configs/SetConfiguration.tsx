/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Switch,
  StyleSheet,
  TouchableOpacity,
  Platform,
} from 'react-native';
import Geolocation from '@react-native-community/geolocation';

const authorizationLevelOptions: ('whenInUse' | 'always' | 'auto')[] = [
  'whenInUse',
  'always',
  'auto',
];

const locationProviderOptions: ('playServices' | 'android' | 'auto')[] = [
  'playServices',
  'android',
  'auto',
];

export default function SetConfigurationExample() {
  const [skipPermissionRequests, setSkipPermissionRequests] = useState(false);
  const [authorizationLevel, setAuthorizationLevel] = useState<
    'whenInUse' | 'always' | 'auto'
  >('auto');
  const [locationProvider, setLocationProvider] = useState<
    'playServices' | 'android' | 'auto'
  >('auto');
  const [enableBackgroundLocationUpdates, setEnableBackgroundLocationUpdates] =
    useState(false);

  useEffect(() => {
    Geolocation.setRNConfiguration({
      skipPermissionRequests,
      authorizationLevel,
      locationProvider,
    });
  }, [skipPermissionRequests, authorizationLevel, locationProvider]);

  return (
    <View>
      <View style={styles.row}>
        <Text>skipPermissionRequests</Text>
        <Switch
          onValueChange={() =>
            setSkipPermissionRequests(!skipPermissionRequests)
          }
          value={skipPermissionRequests}
        />
      </View>
      {Platform.OS === 'ios' && (
        <>
          <View style={styles.row}>
            <Text>authorizationLevel</Text>
            <View style={styles.segmentControlContainer}>
              {authorizationLevelOptions.map((item, index) => (
                <TouchableOpacity
                  key={`segmented-control-${index}`}
                  onPress={() =>
                    setAuthorizationLevel(authorizationLevelOptions[index])
                  }
                  style={[
                    styles.segmentedControlButton,
                    authorizationLevelOptions.indexOf(authorizationLevel) ===
                      index && styles.segmentedControlButtonActive,
                  ]}
                >
                  <Text
                    style={[
                      styles.segmentControlText,
                      authorizationLevelOptions.indexOf(authorizationLevel) ===
                        index && styles.segmentControlTextActive,
                    ]}
                  >
                    {item}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
          <View style={styles.row}>
            <Text>enableBackgroundLocationUpdates</Text>
            <Switch
              onValueChange={() =>
                setEnableBackgroundLocationUpdates(
                  !enableBackgroundLocationUpdates
                )
              }
              value={enableBackgroundLocationUpdates}
            />
          </View>
        </>
      )}
      {Platform.OS === 'android' && (
        <View style={styles.row}>
          <Text>locationProvider</Text>
          <View style={styles.segmentControlContainer}>
            {locationProviderOptions.map((item, index) => (
              <TouchableOpacity
                key={`segmented-control-${index}`}
                onPress={() =>
                  setLocationProvider(locationProviderOptions[index])
                }
                style={[
                  styles.segmentedControlButton,
                  locationProviderOptions.indexOf(locationProvider) === index &&
                    styles.segmentedControlButtonActive,
                ]}
              >
                <Text
                  style={[
                    styles.segmentControlText,
                    locationProviderOptions.indexOf(locationProvider) ===
                      index && styles.segmentControlTextActive,
                  ]}
                >
                  {item}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 5,
  },
  segmentControlContainer: {
    flexDirection: 'row',
  },
  segmentedControlButton: {
    marginHorizontal: 2,
    borderRadius: 5,
    borderWidth: 1,
    borderColor: '#007aff',
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  segmentedControlButtonActive: {
    backgroundColor: '#007aff',
  },
  segmentControlText: {
    color: '#007aff',
  },
  segmentControlTextActive: {
    color: '#fff',
  },
});
