/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

import React, { useEffect, useRef, useState } from 'react';
import { View, Button, AppState } from 'react-native';
import BackgroundTimer from 'react-native-background-timer';
import Geolocation from '@react-native-community/geolocation';

export default function BackgroundLocationUpdates() {
  const appState = useRef(AppState.currentState);
  const [backgroundListener, setBackgroundListener] = useState(false);

  useEffect(() => {
    if (!backgroundListener) {
      return;
    }

    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState.match(/inactive|background/)) {
        BackgroundTimer.runBackgroundTimer(() => {
          Geolocation.getCurrentPosition(
            (position) => {
              console.log(
                'getCurrentPosition background',
                JSON.stringify(position)
              );
            },
            (error) =>
              console.log(
                'getCurrentPosition background error',
                JSON.stringify(error)
              ),
            { enableHighAccuracy: true }
          );
        }, 1000);

        console.log('App has come to the foreground!');
      } else {
        BackgroundTimer.stopBackgroundTimer();
      }

      appState.current = nextAppState;
    });

    return () => {
      subscription.remove();
    };
  }, [backgroundListener]);

  return (
    <View>
      <Button
        title={`${
          backgroundListener ? 'Disable' : 'Enable'
        } background location updates`}
        onPress={() => setBackgroundListener(!backgroundListener)}
      />
    </View>
  );
}
