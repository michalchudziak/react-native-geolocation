package com.reactnativecommunity.geolocation;

import com.facebook.react.bridge.ReadableMap;

public class LocationOptions {
    protected static final float RCT_DEFAULT_LOCATION_ACCURACY = 100;
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