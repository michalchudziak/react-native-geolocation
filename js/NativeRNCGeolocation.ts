import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type GeolocationConfiguration = {
  skipPermissionRequests: boolean;
  authorizationLevel?: 'always' | 'whenInUse' | 'auto';
  locationProvider?: 'playServices' | 'android' | 'auto';
};

export type GeolocationOptions = {
  timeout?: number;
  maximumAge?: number;
  enableHighAccuracy?: boolean;
  distanceFilter?: number;
  useSignificantChanges?: boolean;
  interval?: number;
  fastestInterval?: number;
};

export type GeolocationResponse = {
  coords: {
    latitude: number;
    longitude: number;
    altitude: number | null;
    accuracy: number;
    altitudeAccuracy: number | null;
    heading: number | null;
    speed: number | null;
  };
  timestamp: number;
};

export type GeolocationError = {
  code: number;
  message: string;
  PERMISSION_DENIED: number;
  POSITION_UNAVAILABLE: number;
  TIMEOUT: number;
};

export interface Spec extends TurboModule {
  setConfiguration(config: {
    skipPermissionRequests: boolean;
    authorizationLevel?: string;
  }): void;
  requestAuthorization(
    success: () => void,
    error: (error: GeolocationError) => void
  ): void;
  getCurrentPosition(
    options: GeolocationOptions,
    position: (position: GeolocationResponse) => void,
    error: (error: GeolocationError) => void
  ): void;
  startObserving(options: GeolocationOptions): void;
  stopObserving(): void;

  // RCTEventEmitter
  addListener: (eventName: string) => void;
  removeListeners: (count: number) => void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RNCGeolocation');
