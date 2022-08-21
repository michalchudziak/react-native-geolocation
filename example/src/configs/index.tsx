import React from 'react';
import { Platform } from 'react-native';
import RequestAuthorization from './RequestAuthorization';
import SetConfiguration from './SetConfiguration';

const configs = [
  {
    id: 'setRNConfiguration',
    title: 'setRNConfiguration()',
    description:
      'Sets the configuration for the react-native-geolocation module',
    render() {
      return <SetConfiguration />;
    },
  },
];

if (Platform.OS === 'ios') {
  configs.push({
    id: 'requestAuthorization',
    title: 'requestAuthorization()',
    description: 'Request authorization for location services',
    render() {
      return <RequestAuthorization />;
    },
  });
}

export default configs;
