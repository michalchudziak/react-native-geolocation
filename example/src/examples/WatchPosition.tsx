/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, Alert, Button } from 'react-native';
import Geolocation, { type GeolocationResponse } from '@react-native-community/geolocation';
import {
  WatchOptionsForm,
  buildCurrentPositionOptions,
  buildWatchOptions,
  initialWatchOptionValues,
  type WatchOptionFormValues,
} from '../components/WatchOptionsForm';

export default function WatchPositionExample() {
  const [formValues, setFormValues] =
    useState<WatchOptionFormValues>(initialWatchOptionValues);
  const [position, setPosition] =
    useState<GeolocationResponse | null>(null);
  const [subscriptionId, setSubscriptionId] = useState<number | null>(null);

  const watchPosition = () => {
    try {
      const currentOptions = buildCurrentPositionOptions(formValues);
      console.log('watchPosition.getCurrentPositionOptions', currentOptions);
      Geolocation.getCurrentPosition(
        (nextPosition) => {
          setPosition(nextPosition);
        },
        (error) => Alert.alert('GetCurrentPosition Error', JSON.stringify(error)),
        currentOptions
      );

      const watchOptions = buildWatchOptions(formValues);
      console.log('watchPosition.startOptions', watchOptions);
      const watchID = Geolocation.watchPosition(
        (nextPosition) => {
          console.log('watchPosition', JSON.stringify(nextPosition));
          setPosition(nextPosition);
        },
        (error) => Alert.alert('WatchPosition Error', JSON.stringify(error)),
        watchOptions
      );
      setSubscriptionId(watchID);
    } catch (error) {
      Alert.alert('WatchPosition Error', JSON.stringify(error));
    }
  };

  const clearWatch = () => {
    subscriptionId !== null && Geolocation.clearWatch(subscriptionId);
    setSubscriptionId(null);
    setPosition(null);
  };

  useEffect(() => {
    return () => {
      clearWatch();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View>
      <WatchOptionsForm
        values={formValues}
        onChange={(field, value) =>
          setFormValues((prev) => ({ ...prev, [field]: value }))
        }
      />
      <Text>
        <Text style={styles.title}>Last position: </Text>
        {position ? JSON.stringify(position) : 'unknown'}
      </Text>
      {position && (
        <Text style={styles.caption}>
          Position timestamp:{' '}
          {new Date(position.timestamp).toLocaleTimeString()}
        </Text>
      )}
      {subscriptionId !== null ? (
        <Button title="Clear Watch" onPress={clearWatch} />
      ) : (
        <Button title="Watch Position" onPress={watchPosition} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  title: {
    fontWeight: '500',
  },
  row: {
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  label: {
    flex: 1,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    minWidth: 100,
    textAlign: 'right',
  },
  caption: {
    marginBottom: 12,
    color: '#555',
  },
});
