import React from 'react';
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
  {
    id: 'requestAuthorization',
    title: 'requestAuthorization()',
    description: 'Request authorization for location services',
    render() {
      return <RequestAuthorization />;
    },
  },
];

export default configs;
