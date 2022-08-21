/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import React from 'react';
import { AppRegistry } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { name as appName } from './app.json';
import * as Screens from './src/screens';

const Stack = createNativeStackNavigator();

function ExampleApp() {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="Home" component={Screens.HomeScreen} />
        <Stack.Screen name="Examples" component={Screens.ExamplesScreen} />
        <Stack.Screen name="TestCases" component={Screens.TestCasesScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

AppRegistry.registerComponent(appName, () => ExampleApp);
