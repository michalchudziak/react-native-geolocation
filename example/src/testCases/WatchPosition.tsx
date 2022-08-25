/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

import React, { useState } from 'react';
import { StyleSheet, Text, View, Alert, Button, Platform } from 'react-native';
import Geolocation from '@react-native-community/geolocation';
import type { GeolocationOptions } from '@react-native-community/geolocation';

const TestCase = ({
  title,
  options,
}: {
  title: string;
  options: GeolocationOptions;
}) => {
  const watchPosition = () => {
    try {
      const watchID = Geolocation.watchPosition(
        (position) => {
          setPosition(JSON.stringify(position));
        },
        (error) => Alert.alert('WatchPosition Error', JSON.stringify(error)),
        options
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

  const [position, setPosition] = useState<string | null>(null);
  const [subscriptionId, setSubscriptionId] = useState<number | null>(null);

  return (
    <View>
      <Text style={styles.title}>{title}</Text>
      <Text>
        <Text style={styles.title}>Position: </Text>
        {position || 'unknown'}
      </Text>
      {subscriptionId !== null ? (
        <Button title="Clear Watch" onPress={clearWatch} />
      ) : (
        <Button title="Watch Position" onPress={watchPosition} />
      )}
    </View>
  );
};

export default function WatchPositionExample() {
  return (
    <>
      <TestCase title="High accuracy" options={{ enableHighAccuracy: true }} />
      <TestCase title="Low accuracy" options={{ enableHighAccuracy: false }} />
      <TestCase title="Timeout 0ms" options={{ timeout: 0 }} />
      <TestCase title="Timeout 10s" options={{ timeout: 10000 }} />
      <TestCase title="Maximum age 0ms" options={{ maximumAge: 0 }} />
      <TestCase title="Maximum age 0s" options={{ maximumAge: 10000 }} />
      <TestCase title="Distance filter 10m" options={{ distanceFilter: 10 }} />
      <TestCase
        title="Using significant changes"
        options={{ useSignificantChanges: true }}
      />
      {Platform.OS === 'android' && (
        <TestCase
          title="Interval 2s, Fastest Interval 1s"
          options={{ interval: 2, fastestInterval: 1 }}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  title: {
    fontWeight: '500',
  },
});
