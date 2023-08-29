import React from 'react';
import RequestAuthorization from './RequestAuthorization';
import SetConfiguration from './SetConfiguration';
import BackgroundLocationUpdates from './BackgroundLocationUpdates';

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
  {
    id: 'backgroundLocationUpdates',
    title: 'getCurrentLoaction() in background',
    description: 'Test background location updates',
    render() {
      return <BackgroundLocationUpdates />;
    },
  },
];

export default configs;
