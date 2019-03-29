
# react-native-geolocation

## Getting started

`$ npm install react-native-geolocation --save`

### Mostly automatic installation

`$ react-native link react-native-geolocation`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-geolocation` and add `RNCGeolocation.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCGeolocation.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactnativecommunity.geolocation.RNCGeolocationPackage;` to the imports at the top of the file
  - Add `new RNCGeolocationPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-geolocation'
  	project(':react-native-geolocation').projectDir = new File(rootProject.projectDir, 	'../../node_modules/react-native-geolocation/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-geolocation')
  	```


## Usage
```javascript
import RNCGeolocation from 'react-native-geolocation';

// TODO: What to do with the module?
RNCGeolocation;
```
