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
import { StyleSheet, Text, View, Alert, Button } from 'react-native';
import Geolocation from '@react-native-community/geolocation';
import type { GeolocationOptions } from '@react-native-community/geolocation';

const TestCase = ({
  title,
  options,
}: {
  title: string;
  options: GeolocationOptions;
}) => {
  const getCurrentPosition = () => {
    Geolocation.getCurrentPosition(
      (pos) => {
        setPosition(JSON.stringify(pos));
      },
      (error) => Alert.alert('GetCurrentPosition Error', JSON.stringify(error)),
      options
    );
  };

  const [position, setPosition] = useState<string | null>(null);

  return (
    <View>
      <Text style={styles.title}>{title}</Text>
      <Text>
        <Text style={styles.title}>Position: </Text>
        {position || 'unknown'}
      </Text>
      <Button title="Test" onPress={getCurrentPosition} />
    </View>
  );
};

export default function GetCurrentLocationExample() {
  return (
    <>
      <TestCase title="High accuracy" options={{ enableHighAccuracy: true }} />
      <TestCase title="Low accuracy" options={{ enableHighAccuracy: false }} />
      <TestCase title="Timeout 0ms" options={{ timeout: 0 }} />
      <TestCase title="Timeout 10s" options={{ timeout: 10000 }} />
      <TestCase title="Maximum age 0ms" options={{ maximumAge: 0 }} />
      <TestCase title="Maximum age 0s" options={{ maximumAge: 10000 }} />
    </>
  );
}

const styles = StyleSheet.create({
  title: {
    fontWeight: '500',
  },
});
