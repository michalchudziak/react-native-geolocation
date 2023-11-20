/**
 * MIT License
 *
 * Copyright (C) 2023 Huawei Device Co., Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANT KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { TurboModule, TurboModuleContext } from 'rnoh/ts';
import geoLocationManager from '@ohos.geoLocationManager';
import bundleManager from '@ohos.bundle.bundleManager';
import abilityAccessCtrl, { Permissions } from '@ohos.abilityAccessCtrl';
// import { PermissionRequestResult } from '@ohos.abilityAccessCtrl';
import { BusinessError } from '@ohos.base';
import { Config, GeolocationOptions } from './Config';
import { LocationManager } from './LocationManager';
import logger from './Logger';

const TAG = "RNCGeolocationTurboModule"

export class RNCGeolocationTurboModule extends TurboModule {
  mConfiguration: Config
  mUiCtx: Context
  mLocationManager: LocationManager

  constructor(protected ctx: TurboModuleContext) {
    super(ctx);
    logger.debug(TAG, ",RNCGeolocationTurboModule constructor");
    this.mUiCtx = ctx.uiAbilityContext
    this.mLocationManager = new LocationManager()
  }

  setConfiguration(config: {
    skipPermissionRequests: boolean;
    authorizationLevel?: string;
    enableBackgroundLocationUpdates?: string;
  }): void {
    logger.debug(TAG, ",call setConfiguration");
    logger.debug(TAG, `,setConfigurationParam:${config.skipPermissionRequests}`);
    this.mConfiguration = config;
  }

  requestAuthorization(
    success: () => void,
    error: (error) => void
  ): void {
    logger.debug(TAG, ",call requestAuthorization");
    const permissions: Array<Permissions> = ['ohos.permission.APPROXIMATELY_LOCATION', 'ohos.permission.LOCATION'];
    let onGrantedSuccess = () => {
      logger.debug(TAG, `,call requestAuthorization,onGranted ok:`);
      logger.debug(TAG, `,call requestAuthorization,onGranted before notify RN:`);
      success();
    }
    let onGrantedFailed = (errorB) => {
      logger.debug(TAG, `,call requestAuthorization,onGrantedFailed error: ${JSON.stringify(errorB)}`);
      error(errorB)
    }
    this.reqPermissionsFromUser(permissions, onGrantedSuccess, onGrantedFailed)
  }

  getCurrentPosition(
    options: GeolocationOptions,
    success: (position) => void,
    error: (error) => void
  ): void {
    logger.debug(TAG, `,call getCurrentPosition`);
    if (this.mConfiguration.skipPermissionRequests) { //问题65rom 异常退出
      //直接获取
      logger.debug(TAG, `,call getCurrentPosition flag100`)
      this.mLocationManager.getCurrentLocationData(options, success, error);
      return;
    }
    logger.debug(TAG, `,call getCurrentPosition,to requestAuthorization ==req200`);
    this.requestAuthorization(() => {
      this.mLocationManager.getCurrentLocationData(options, success, error)
    }, error)
  }

  startObserving(options): void {
    logger.debug(TAG, `,call startObserving`);
    let requestInfo: geoLocationManager.LocationRequest = {
      'priority': geoLocationManager.LocationRequestPriority.FIRST_FIX,
      'scenario': geoLocationManager.LocationRequestScenario.UNSET,
      'timeInterval': 1,
      'distanceInterval': 0,
      'maxAccuracy': 0,
    };
    if (options) {
      if (options.interval) {
        requestInfo.timeInterval = options.interval / 1000
      }
      if (options.distanceFilter) {
        requestInfo.distanceInterval = options.distanceInterval
      } else {
        requestInfo.distanceInterval = 100
      }
      if (options.enableHighAccuracy) {
        requestInfo.maxAccuracy = 0
      }
    }
    this.mLocationManager.startObserving(requestInfo)
  }

  stopObserving(): void {
    logger.debug(TAG, `,call stopObserving`);
    this.mLocationManager.stopObserving()
  }

  addListener(eventName: string): void {
    logger.debug(TAG, `,call addListener`);
  }

  removeListeners(count: number): void {
    logger.debug(TAG, `,call removeListeners`);
  }

  async checkAccessToken(permission: Permissions): Promise<abilityAccessCtrl.GrantStatus> {
    let atManager: abilityAccessCtrl.AtManager = abilityAccessCtrl.createAtManager();
    let grantStatus: abilityAccessCtrl.GrantStatus = abilityAccessCtrl.GrantStatus.PERMISSION_DENIED;
    //获取应用程序的accessTokenID
    let tokenId: number = 0;
    try {
      let bundleInfo: bundleManager.BundleInfo = await bundleManager.getBundleInfoForSelf(bundleManager.BundleFlag.GET_BUNDLE_INFO_WITH_APPLICATION);
      let appInfo: bundleManager.ApplicationInfo = bundleInfo.appInfo;
      tokenId = appInfo.accessTokenId;
    } catch (error) {
      let err: BusinessError = error as BusinessError;
      logger.error(TAG, `checkAccessToken,Failed to get bundle info for self. Code is ${err.code}, message is ${err.message}`);
    }
    // 校验应用是否被授予权限
    try {
      grantStatus = await atManager.checkAccessToken(tokenId, permission);
    } catch (error) {
      let err: BusinessError = error as BusinessError;
      logger.error(TAG, `checkAccessToken,Failed to check access token. Code is ${err.code}, message is ${err.message}`);
    }
    return grantStatus;
  }

  async checkPermissions(): Promise<boolean> {
    const permissions: Array<Permissions> = ['ohos.permission.APPROXIMATELY_LOCATION', 'ohos.permission.LOCATION'];
    logger.debug(TAG, `checkPermissions,flag100`);
    let grantStatus: abilityAccessCtrl.GrantStatus = await this.checkAccessToken(permissions[0]);
    logger.debug(TAG, `checkPermissions,flag200`);
    let grantStatus2: abilityAccessCtrl.GrantStatus = await this.checkAccessToken(permissions[1]);
    logger.debug(TAG, `checkPermissions,flag300`);
    if (grantStatus === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED
      && grantStatus2 === abilityAccessCtrl.GrantStatus.PERMISSION_GRANTED) {
      logger.debug(TAG, `checkPermissions,flag500`);
      return true
    } else {
      return false
    }
  }

  reqPermissionsFromUser(permissions: Array<Permissions>, onGrantedSuccess: () => void, onGrantedFailed: (error) => void): void {
    let context: Context = this.mUiCtx
    let atManager: abilityAccessCtrl.AtManager = abilityAccessCtrl.createAtManager();
    atManager.requestPermissionsFromUser(context, permissions).then((data) => {
      let grantStatus: Array<number> = data.authResults;
      let length: number = grantStatus.length;
      let grantedCount = 0;
      for (let i = 0; i < length; i++) {
        if (grantStatus[i] === 0) {
          logger.debug(TAG, `,reqPermissionsFromUser,granted true: ${i}`);
          grantedCount++;
        } else {
          logger.debug(TAG, `,reqPermissionsFromUser,granted false: ${i}`);
        }
      }
      if (grantedCount == permissions.length) {
        // 授权成功
        logger.debug(TAG, `,reqPermissionsFromUser,granted ok100`);
        onGrantedSuccess()
      } else {
        onGrantedFailed({ message: "部分权限未获授权" });
      }
    }).catch((err: BusinessError) => {
      logger.error(TAG, `,reqPermissionsFromUser,Failed to request permissions from user. Code is ${err.code}, message is ${err.message}`);
    })
  }
}