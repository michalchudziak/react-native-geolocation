/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "RNCGeolocation.h"

#import <CoreLocation/CLError.h>
#import <CoreLocation/CLLocationManager.h>
#import <CoreLocation/CLLocationManagerDelegate.h>

#import <React/RCTAssert.h>
#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>

typedef NS_ENUM(NSInteger, RNCPositionErrorCode) {
  RNCPositionErrorDenied = 1,
  RNCPositionErrorUnavailable,
  RNCPositionErrorTimeout,
};

typedef NS_ENUM(NSInteger, RNCGeolocationAuthorizationLevel) {
  RNCGeolocationAuthorizationLevelDefault,
  RNCGeolocationAuthorizationLevelWhenInUse,
  RNCGeolocationAuthorizationLevelAlways,
};

#define RNC_DEFAULT_LOCATION_ACCURACY kCLLocationAccuracyHundredMeters

typedef struct {
  BOOL skipPermissionRequests;
  RNCGeolocationAuthorizationLevel authorizationLevel;
} RNCGeolocationConfiguration;

typedef struct {
  double timeout;
  double maximumAge;
  double accuracy;
  double distanceFilter;
  BOOL useSignificantChanges;
} RNCGeolocationOptions;

@implementation RCTConvert (RNCGeolocationAuthorizationLevel)
RCT_ENUM_CONVERTER(RNCGeolocationAuthorizationLevel, (@{
    @"whenInUse": @(RNCGeolocationAuthorizationLevelWhenInUse),
    @"always": @(RNCGeolocationAuthorizationLevelAlways)}),
  RNCGeolocationAuthorizationLevelDefault, integerValue)
@end

@implementation RCTConvert (RNCGeolocationOptions)

+ (RNCGeolocationConfiguration)RNCGeolocationConfiguration:(id)json
{
  NSDictionary<NSString *, id> *options = [RCTConvert NSDictionary:json];

  return (RNCGeolocationConfiguration) {
    .skipPermissionRequests = [RCTConvert BOOL:options[@"skipPermissionRequests"]],
    .authorizationLevel = [RCTConvert RNCGeolocationAuthorizationLevel:options[@"authorizationLevel"]]
  };
}

+ (RNCGeolocationOptions)RNCGeolocationOptions:(id)json
{
  NSDictionary<NSString *, id> *options = [RCTConvert NSDictionary:json];

  double distanceFilter = options[@"distanceFilter"] == NULL ? RNC_DEFAULT_LOCATION_ACCURACY
  : [RCTConvert double:options[@"distanceFilter"]] ?: kCLDistanceFilterNone;

  return (RNCGeolocationOptions){
    .timeout = [RCTConvert NSTimeInterval:options[@"timeout"]] ?: INFINITY,
    .maximumAge = [RCTConvert NSTimeInterval:options[@"maximumAge"]] ?: INFINITY,
    .accuracy = [RCTConvert BOOL:options[@"enableHighAccuracy"]] ? kCLLocationAccuracyBest : RNC_DEFAULT_LOCATION_ACCURACY,
    .distanceFilter = distanceFilter,
    .useSignificantChanges = [RCTConvert BOOL:options[@"useSignificantChanges"]] ?: NO,
  };
}

@end

static NSDictionary<NSString *, id> *RNCPositionError(RNCPositionErrorCode code, NSString *msg /* nil for default */)
{
  if (!msg) {
    switch (code) {
      case RNCPositionErrorDenied:
        msg = @"User denied access to location services.";
        break;
      case RNCPositionErrorUnavailable:
        msg = @"Unable to retrieve location.";
        break;
      case RNCPositionErrorTimeout:
        msg = @"The location request timed out.";
        break;
    }
  }

  return @{
           @"code": @(code),
           @"message": msg,
           @"PERMISSION_DENIED": @(RNCPositionErrorDenied),
           @"POSITION_UNAVAILABLE": @(RNCPositionErrorUnavailable),
           @"TIMEOUT": @(RNCPositionErrorTimeout)
           };
}

@interface RNCGeolocationRequest : NSObject

@property (nonatomic, copy) RCTResponseSenderBlock successBlock;
@property (nonatomic, copy) RCTResponseSenderBlock errorBlock;
@property (nonatomic, assign) RNCGeolocationOptions options;
@property (nonatomic, strong) NSTimer *timeoutTimer;

@end

@implementation RNCGeolocationRequest

- (void)dealloc
{
  if (_timeoutTimer.valid) {
    [_timeoutTimer invalidate];
  }
}

@end

@interface RNCGeolocation () <CLLocationManagerDelegate>

@end

@implementation RNCGeolocation
{
  CLLocationManager *_locationManager;
  NSDictionary<NSString *, id> *_lastLocationEvent;
  NSMutableArray<RNCGeolocationRequest *> *_pendingRequests;
  BOOL _observingLocation;
  BOOL _usingSignificantChanges;
  RNCGeolocationConfiguration _locationConfiguration;
  RNCGeolocationOptions _observerOptions;
  CLAuthorizationStatus _lastUpdatedAuthorizationStatus; // used since iOS 14.0+
}

RCT_EXPORT_MODULE()

#pragma mark - Lifecycle

- (void)dealloc
{
  [self stopMonitoring];

  _locationManager.delegate = nil;
}

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"geolocationDidChange", @"geolocationError"];
}

#pragma mark - Private API

- (void)beginLocationUpdatesWithDesiredAccuracy:(CLLocationAccuracy)desiredAccuracy distanceFilter:(CLLocationDistance)distanceFilter useSignificantChanges:(BOOL)useSignificantChanges
{
  if (!_locationConfiguration.skipPermissionRequests) {
    [self requestAuthorization];
  }

  if (!_locationManager) {
    _locationManager = [CLLocationManager new];
    _locationManager.delegate = self;
  }

  _locationManager.distanceFilter  = distanceFilter;
  _locationManager.desiredAccuracy = desiredAccuracy;
  _usingSignificantChanges = useSignificantChanges;

  if (@available(iOS 14.0, *)) {
    if (
      _lastUpdatedAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways ||
      _lastUpdatedAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse
    ) {
      [self startMonitoring];
    }
  } else {
    [self startMonitoring];
  }
}

- (void)startMonitoring
{
  _usingSignificantChanges
    ? [_locationManager startMonitoringSignificantLocationChanges]
    : [_locationManager startUpdatingLocation];
}

- (void)stopMonitoring
{
  _usingSignificantChanges
    ? [_locationManager stopMonitoringSignificantLocationChanges]
    : [_locationManager stopUpdatingLocation];
}

#pragma mark - Timeout handler

- (void)timeout:(NSTimer *)timer
{
  RNCGeolocationRequest *request = timer.userInfo;
  NSString *message = [NSString stringWithFormat: @"Unable to fetch location within %.1fs.", request.options.timeout];
  request.errorBlock(@[RNCPositionError(RNCPositionErrorTimeout, message)]);
  [_pendingRequests removeObject:request];

  // Stop updating if no pending requests
  if (_pendingRequests.count == 0 && !_observingLocation) {
    [self stopMonitoring];
  }
}

#pragma mark - Public API

RCT_EXPORT_METHOD(setConfiguration:(RNCGeolocationConfiguration)config)
{
  _locationConfiguration = config;
}

RCT_EXPORT_METHOD(requestAuthorization)
{
  if (!_locationManager) {
    _locationManager = [CLLocationManager new];
    _locationManager.delegate = self;
  }
  BOOL wantsAlways = NO;
  BOOL wantsWhenInUse = NO;
  if (_locationConfiguration.authorizationLevel == RNCGeolocationAuthorizationLevelDefault) {
    if ([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationAlwaysUsageDescription"] &&
        [_locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
      wantsAlways = YES;
    } else if ([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationWhenInUseUsageDescription"] &&
               [_locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
      wantsWhenInUse = YES;
    }
  } else if (_locationConfiguration.authorizationLevel == RNCGeolocationAuthorizationLevelAlways) {
    wantsAlways = YES;
  } else if (_locationConfiguration.authorizationLevel == RNCGeolocationAuthorizationLevelWhenInUse) {
    wantsWhenInUse = YES;
  }

  // Request location access permission
  if (wantsAlways) {
    [_locationManager requestAlwaysAuthorization];

    // On iOS 9+ we also need to enable background updates
    NSArray *backgroundModes  = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIBackgroundModes"];
    if (backgroundModes && [backgroundModes containsObject:@"location"]) {
      if ([_locationManager respondsToSelector:@selector(setAllowsBackgroundLocationUpdates:)]) {
        [_locationManager setAllowsBackgroundLocationUpdates:YES];
      }
    }
  } else if (wantsWhenInUse) {
    [_locationManager requestWhenInUseAuthorization];
  }
}

RCT_EXPORT_METHOD(startObserving:(RNCGeolocationOptions)options)
{
  checkLocationConfig();

  // Select best options
  _observerOptions = options;
  for (RNCGeolocationRequest *request in _pendingRequests) {
    _observerOptions.accuracy = MIN(_observerOptions.accuracy, request.options.accuracy);
  }

  [self beginLocationUpdatesWithDesiredAccuracy:_observerOptions.accuracy
                                 distanceFilter:_observerOptions.distanceFilter
                          useSignificantChanges:_observerOptions.useSignificantChanges];
  _observingLocation = YES;
}

RCT_EXPORT_METHOD(stopObserving)
{
  // Stop observing
  _observingLocation = NO;

  // Stop updating if no pending requests
  if (_pendingRequests.count == 0) {
    [self stopMonitoring];
  }
}

RCT_EXPORT_METHOD(getCurrentPosition:(RNCGeolocationOptions)options
                  withSuccessCallback:(RCTResponseSenderBlock)successBlock
                  errorCallback:(RCTResponseSenderBlock)errorBlock)
{
  checkLocationConfig();

  if (!successBlock) {
    RCTLogError(@"%@.getCurrentPosition called with nil success parameter.", [self class]);
    return;
  }

  if (errorBlock) {
    if (@available(iOS 14.0, *)) {
      if (_lastUpdatedAuthorizationStatus == kCLAuthorizationStatusRestricted) {
        errorBlock(@[RNCPositionError(RNCPositionErrorUnavailable, @"This application is not authorized to use location services")]);
        return;
      }

      if (_lastUpdatedAuthorizationStatus == kCLAuthorizationStatusDenied) {
        errorBlock(@[RNCPositionError(RNCPositionErrorDenied, nil)]);
        return;
      }
    } else {
      if (![CLLocationManager locationServicesEnabled]) {
        errorBlock(@[RNCPositionError(RNCPositionErrorUnavailable, @"Location services disabled.")]);
        return;
      }

      if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusDenied) {
        errorBlock(@[RNCPositionError(RNCPositionErrorDenied, nil)]);
        return;
      }
    }
  }

  // Check if previous recorded location exists and is good enough
  if (_lastLocationEvent &&
      [NSDate date].timeIntervalSince1970 - [RCTConvert NSTimeInterval:_lastLocationEvent[@"timestamp"]] < options.maximumAge &&
      [_lastLocationEvent[@"coords"][@"accuracy"] doubleValue] <= options.accuracy) {
    // Call success block with most recent known location
    successBlock(@[_lastLocationEvent]);
    return;
  }

  // Create request
  RNCGeolocationRequest *request = [RNCGeolocationRequest new];
  request.successBlock = successBlock;
  request.errorBlock = errorBlock ?: ^(NSArray *args){};
  request.options = options;
  request.timeoutTimer = [NSTimer scheduledTimerWithTimeInterval:options.timeout
                                                          target:self
                                                        selector:@selector(timeout:)
                                                        userInfo:request
                                                         repeats:NO];
  if (!_pendingRequests) {
    _pendingRequests = [NSMutableArray new];
  }
  [_pendingRequests addObject:request];

  // Configure location manager and begin updating location
  CLLocationAccuracy accuracy = options.accuracy;
  if (_locationManager) {
    accuracy = MIN(_locationManager.desiredAccuracy, accuracy);
  }
  [self beginLocationUpdatesWithDesiredAccuracy:accuracy
                                 distanceFilter:options.distanceFilter
                          useSignificantChanges:options.useSignificantChanges];
}

#pragma mark - CLLocationManagerDelegate

- (void)locationManager:(CLLocationManager *)manager
     didUpdateLocations:(NSArray<CLLocation *> *)locations
{
  // Create event
  CLLocation *location = locations.lastObject;
  _lastLocationEvent = @{
                         @"coords": @{
                             @"latitude": @(location.coordinate.latitude),
                             @"longitude": @(location.coordinate.longitude),
                             @"altitude": @(location.altitude),
                             @"accuracy": @(location.horizontalAccuracy),
                             @"altitudeAccuracy": @(location.verticalAccuracy),
                             @"heading": @(location.course),
                             @"speed": @(location.speed),
                             },
                         @"timestamp": @([location.timestamp timeIntervalSince1970] * 1000) // in ms
                         };

  // Send event
  if (_observingLocation) {
    [self sendEventWithName:@"geolocationDidChange" body:_lastLocationEvent];
  }

  // Fire all queued callbacks
  for (RNCGeolocationRequest *request in _pendingRequests) {
    request.successBlock(@[_lastLocationEvent]);
    [request.timeoutTimer invalidate];
  }
  [_pendingRequests removeAllObjects];

  // Stop updating if not observing
  if (!_observingLocation) {
    [self stopMonitoring];
  }

}

- (void)locationManagerDidChangeAuthorization:(CLLocationManager *)manager
{
  if (@available(iOS 14.0, *)) {
    if (
      manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways ||
      manager.authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse
    ) {
      [self startMonitoring];
    } else {
      NSDictionary<NSString *, id> *jsError = manager.authorizationStatus == kCLAuthorizationStatusRestricted
        ? RNCPositionError(RNCPositionErrorUnavailable, @"This application is not authorized to use location services")
        : manager.authorizationStatus == kCLAuthorizationStatusDenied
          ? RNCPositionError(RNCPositionErrorDenied, nil)
          : nil;

      if (jsError != nil) {
        if (_observingLocation) {
          [self sendEventWithName:@"geolocationError" body:jsError];
        }

        // Fire all queued error callbacks
        for (RNCGeolocationRequest *request in _pendingRequests) {
          request.errorBlock(@[jsError]);
          [request.timeoutTimer invalidate];
        }
        [_pendingRequests removeAllObjects];
      }

      // Stop updating if user has explicitly denied authorization for this application, or
      // location services are disabled in Settings, or any other reason.
      [self stopMonitoring];
    }

    _lastUpdatedAuthorizationStatus = manager.authorizationStatus;
  }
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
  // Check error type
  NSDictionary<NSString *, id> *jsError = nil;
  switch (error.code) {
    case kCLErrorDenied:
      jsError = RNCPositionError(RNCPositionErrorDenied, nil);
      break;
    case kCLErrorNetwork:
      jsError = RNCPositionError(RNCPositionErrorUnavailable, @"Unable to retrieve location due to a network failure");
      break;
    case kCLErrorLocationUnknown:
    default:
      jsError = RNCPositionError(RNCPositionErrorUnavailable, nil);
      break;
  }

  // Send event
  if (_observingLocation) {
    [self sendEventWithName:@"geolocationError" body:jsError];
  }

  // Fire all queued error callbacks
  for (RNCGeolocationRequest *request in _pendingRequests) {
    request.errorBlock(@[jsError]);
    [request.timeoutTimer invalidate];
  }
  [_pendingRequests removeAllObjects];

}

static void checkLocationConfig()
{
#if RCT_DEV
  if (!([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationWhenInUseUsageDescription"] ||
        [[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationAlwaysUsageDescription"] ||
        [[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationAlwaysAndWhenInUseUsageDescription"])) {
    RCTLogError(@"Either NSLocationWhenInUseUsageDescription or NSLocationAlwaysUsageDescription or NSLocationAlwaysAndWhenInUseUsageDescription key must be present in Info.plist to use geolocation.");
  }
#endif
}

@end
