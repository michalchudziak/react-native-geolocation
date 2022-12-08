package com.reactnativecommunity.geolocation;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public abstract class BaseLocationManager {
    protected static final float RCT_DEFAULT_LOCATION_ACCURACY = 100;
    public ReactApplicationContext mReactContext;

    protected BaseLocationManager(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    protected static WritableMap locationToMap(Location location) {
        WritableMap map = Arguments.createMap();
        WritableMap coords = Arguments.createMap();
        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        coords.putDouble("altitude", location.getAltitude());
        coords.putDouble("accuracy", location.getAccuracy());
        coords.putDouble("heading", location.getBearing());
        coords.putDouble("speed", location.getSpeed());
        map.putMap("coords", coords);
        map.putDouble("timestamp", location.getTime());

        Bundle bundle = location.getExtras();
        if (bundle != null) {
            WritableMap extras = Arguments.createMap();
            for (String key: bundle.keySet()) {
                putIntoMap(extras, key, bundle.get(key));
            }

            map.putMap("extras", extras);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            map.putBoolean("mocked", location.isFromMockProvider());
        }

        return map;
    }

    protected static void putIntoMap(WritableMap map, String key, Object value) {
        if (value instanceof Integer) {
            map.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            map.putInt(key, ((Long) value).intValue());
        } else if (value instanceof Float) {
            map.putDouble(key, (Float) value);
        } else if (value instanceof Double) {
            map.putDouble(key, (Double) value);
        } else if (value instanceof String) {
            map.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            map.putBoolean(key, (Boolean) value);
        } else if (value instanceof int[]
                || value instanceof long[]
                || value instanceof double[]
                || value instanceof String[]
                || value instanceof boolean[]) {
            map.putArray(key,  Arguments.fromArray(value));
        }
    }

    protected void emitError(int code, String message) {
        mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("geolocationError", PositionError.buildError(code, message));
    }

    abstract public void getCurrentLocationData(ReadableMap options, final Callback success, Callback error);
    abstract public void startObserving(ReadableMap options);
    abstract public void stopObserving();

    protected static class LocationOptions {
        protected final int interval;
        protected final int fastestInterval;
        protected final long timeout;
        protected final double maximumAge;
        protected final boolean highAccuracy;
        protected final float distanceFilter;

        private LocationOptions(
                int interval,
                int fastestInterval,
                long timeout,
                double maximumAge,
                boolean highAccuracy,
                float distanceFilter) {
            this.interval = interval;
            this.fastestInterval = fastestInterval;
            this.timeout = timeout;
            this.maximumAge = maximumAge;
            this.highAccuracy = highAccuracy;
            this.distanceFilter = distanceFilter;
        }

        protected static LocationOptions fromReactMap(ReadableMap map) {
            // precision might be dropped on timeout (double -> int conversion), but that's OK
            int interval =
                    map.hasKey("interval") ? map.getInt("interval") : 10000;
            int fastestInterval =
                    map.hasKey("fastestInterval") ? map.getInt("fastestInterval") : -1;
            long timeout =
                    map.hasKey("timeout") ? (long) map.getDouble("timeout") : 1000 * 60 * 10;
            double maximumAge =
                    map.hasKey("maximumAge") ? map.getDouble("maximumAge") : Double.POSITIVE_INFINITY;
            boolean highAccuracy =
                    map.hasKey("enableHighAccuracy") && map.getBoolean("enableHighAccuracy");
            float distanceFilter = map.hasKey("distanceFilter") ?
                    (float) map.getDouble("distanceFilter") :
                    RCT_DEFAULT_LOCATION_ACCURACY;

            return new LocationOptions(interval, fastestInterval, timeout, maximumAge, highAccuracy, distanceFilter);
        }
    }
}
