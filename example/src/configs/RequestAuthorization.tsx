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
import { View, Button } from 'react-native';
import Geolocation from '@react-native-community/geolocation';

export default function RequestAuthorizationExample() {
  const requestAuthorization = () => {
    Geolocation.requestAuthorization();
  };

  return (
    <View>
      <Button title="Request Authorization" onPress={requestAuthorization} />
    </View>
  );
}
