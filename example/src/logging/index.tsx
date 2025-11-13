import React from 'react';
import WatchPositionLogger from './WatchPositionLogger';

export default [
  {
    id: 'watchPositionLogger',
    title: 'watchPosition() logger',
    description:
      'Watch position with CSV logging to external app storage directory',
    render() {
      return <WatchPositionLogger />;
    },
  },
];

