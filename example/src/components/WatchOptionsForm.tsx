/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  Switch,
  TextInput,
  Platform,
} from 'react-native';
import type { GeolocationOptions } from '@react-native-community/geolocation';

export const DEFAULT_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
export const DEFAULT_DISTANCE_FILTER_M = 100;

export type WatchOptionFormValues = {
  enableHighAccuracy: boolean;
  timeout: string;
  maximumAge: string;
  distanceFilter: string;
  useSignificantChanges: boolean;
  interval: string;
  fastestInterval: string;
};

export const initialWatchOptionValues: WatchOptionFormValues = {
  enableHighAccuracy: false,
  timeout: '',
  maximumAge: '',
  distanceFilter: '',
  useSignificantChanges: false,
  interval: '',
  fastestInterval: '',
};

const parseNumber = (value: string) => {
  if (value.trim().length === 0) {
    return undefined;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

export const buildWatchOptions = (
  values: WatchOptionFormValues
): GeolocationOptions => {
  const options: GeolocationOptions = {};

  if (values.enableHighAccuracy) {
    options.enableHighAccuracy = true;
  }

  const timeoutValue = parseNumber(values.timeout);
  if (timeoutValue !== undefined) {
    options.timeout = timeoutValue;
  }

  const maximumAgeValue = parseNumber(values.maximumAge);
  if (maximumAgeValue !== undefined) {
    options.maximumAge = maximumAgeValue;
  }

  const distanceFilterValue = parseNumber(values.distanceFilter);
  if (distanceFilterValue !== undefined) {
    options.distanceFilter = distanceFilterValue;
  }

  if (Platform.OS === 'ios') {
    options.useSignificantChanges = values.useSignificantChanges;
  }

  if (Platform.OS === 'android') {
    const intervalValue = parseNumber(values.interval);
    if (intervalValue !== undefined) {
      options.interval = intervalValue;
    }

    const fastestIntervalValue = parseNumber(values.fastestInterval);
    if (fastestIntervalValue !== undefined) {
      options.fastestInterval = fastestIntervalValue;
    }
  }

  return options;
};

export const buildCurrentPositionOptions = (
  values: WatchOptionFormValues
): GeolocationOptions => {
  const options: GeolocationOptions = {};

  if (values.enableHighAccuracy) {
    options.enableHighAccuracy = true;
  }

  const timeoutValue = parseNumber(values.timeout);
  if (timeoutValue !== undefined) {
    options.timeout = timeoutValue;
  }

  const maximumAgeValue = parseNumber(values.maximumAge);
  if (maximumAgeValue !== undefined) {
    options.maximumAge = maximumAgeValue;
  }

  return options;
};

type Props = {
  values: WatchOptionFormValues;
  onChange: <T extends keyof WatchOptionFormValues>(
    field: T,
    value: WatchOptionFormValues[T]
  ) => void;
};

export function WatchOptionsForm({ values, onChange }: Props) {
  return (
    <>
      <View style={styles.row}>
        <Text style={styles.label}>High accuracy (default: off)</Text>
        <Switch
          value={values.enableHighAccuracy}
          onValueChange={(next) => onChange('enableHighAccuracy', next)}
        />
      </View>
      <View style={styles.row}>
        <Text style={styles.label}>
          Timeout (ms · default: {DEFAULT_TIMEOUT_MS})
        </Text>
        <TextInput
          style={styles.input}
          keyboardType="numeric"
          value={values.timeout}
          onChangeText={(next) => onChange('timeout', next)}
          placeholder={`${DEFAULT_TIMEOUT_MS}`}
        />
      </View>
      <View style={styles.row}>
        <Text style={styles.label}>Maximum age (ms · default: Infinity)</Text>
        <TextInput
          style={styles.input}
          keyboardType="numeric"
          value={values.maximumAge}
          onChangeText={(next) => onChange('maximumAge', next)}
          placeholder="Infinity"
        />
      </View>
      <View style={styles.row}>
        <Text style={styles.label}>
          Distance filter (m · default: {DEFAULT_DISTANCE_FILTER_M})
        </Text>
        <TextInput
          style={styles.input}
          keyboardType="numeric"
          value={values.distanceFilter}
          onChangeText={(next) => onChange('distanceFilter', next)}
          placeholder={`${DEFAULT_DISTANCE_FILTER_M}`}
        />
      </View>
      {Platform.OS === 'ios' && (
        <View style={styles.row}>
          <Text style={styles.label}>Use significant changes (default: false)</Text>
          <Switch
            value={values.useSignificantChanges}
            onValueChange={(next) => onChange('useSignificantChanges', next)}
          />
        </View>
      )}
      {Platform.OS === 'android' && (
        <>
          <View style={styles.row}>
            <Text style={styles.label}>Interval (ms · default: system)</Text>
            <TextInput
              style={styles.input}
              keyboardType="numeric"
              value={values.interval}
              onChangeText={(next) => onChange('interval', next)}
              placeholder="System"
            />
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Fastest interval (ms · default: system)</Text>
            <TextInput
              style={styles.input}
              keyboardType="numeric"
              value={values.fastestInterval}
              onChangeText={(next) => onChange('fastestInterval', next)}
              placeholder="System"
            />
          </View>
        </>
      )}
    </>
  );
}

const styles = StyleSheet.create({
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
});
