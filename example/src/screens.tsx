/**
 * Copyright (c) React Native Community
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import React from 'react';
import {
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import type { ParamListBase } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';

type RenderableExample = {
  id: string;
  title: string;
  description: string;
  render: () => React.ReactElement;
};

// Examples on how to configure the library
import CONFIGS from './configs';

// Examples which show the user how to correctly use the library
import EXAMPLES from './examples';

// Test cases for the e2e tests. THESE ARE NOT EXAMPLES OF BEST PRACTICE
import TEST_CASES from './testCases';

const renderExample = (example: RenderableExample) => {
  return (
    <View
      testID={`example-${example.id}`}
      key={example.title}
      style={styles.exampleContainer}
    >
      <Text style={styles.exampleTitle}>{example.title}</Text>
      <Text style={styles.exampleDescription}>{example.description}</Text>
      <View style={styles.exampleInnerContainer}>{example.render()}</View>
    </View>
  );
};

export function HomeScreen({
  navigation,
}: NativeStackScreenProps<ParamListBase>) {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollContainer}>
        <Text style={styles.sectionTitle}>React Native Geolocation</Text>
        <View style={styles.exampleContainer}>
          <Text style={styles.exampleDescription}>
            Use section below to adjust the configuration or press "Examples" to
            check out the examples.
          </Text>
          <View style={[styles.exampleInnerContainer, styles.buttonContainer]}>
            <Button
              title="Examples"
              onPress={() => navigation.navigate('Examples')}
            />
            <Button
              title="Test cases"
              onPress={() => navigation.navigate('TestCases')}
            />
          </View>
        </View>
        {CONFIGS.map(renderExample)}
      </ScrollView>
    </SafeAreaView>
  );
}

export function ExamplesScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollContainer}>
        <Text style={styles.sectionTitle}>Examples</Text>
        {EXAMPLES.map(renderExample)}
      </ScrollView>
    </SafeAreaView>
  );
}

export function TestCasesScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollContainer}>
        <Text style={styles.sectionTitle}>Test Cases</Text>
        {TEST_CASES.map(renderExample)}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5FCFF',
  },
  scrollContainer: {
    flex: 1,
  },
  sectionTitle: {
    fontSize: 24,
    marginHorizontal: 8,
    marginTop: 24,
  },
  exampleContainer: {
    padding: 16,
    marginVertical: 4,
    backgroundColor: '#FFF',
    borderColor: '#EEE',
    borderTopWidth: 1,
    borderBottomWidth: 1,
  },
  exampleTitle: {
    fontSize: 18,
  },
  exampleDescription: {
    color: '#333333',
    marginBottom: 16,
  },
  exampleInnerContainer: {
    borderColor: '#EEE',
    borderTopWidth: 1,
    paddingTop: 16,
  },
  buttonContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
});
