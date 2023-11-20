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

import geoLocationManager from '@ohos.geoLocationManager';
import { BusinessError } from '@ohos.base';
import logger from './Logger';
import systemDateTime from '@ohos.systemDateTime';

const TAG: string = "LocationManager"

const locationChangeListener = (location: geoLocationManager.Location) => {
  logger.debug(TAG, `locationChangeListener: data:${JSON.stringify(location)}`);
  //todo 传递rn侧
}

/**
 *
 *
 */

export class LocationManager {
  /**
   *
   * @param options
   * @param success
   * @param error
   */
  getCurrentLocationData(options, success, error): void {
    logger.debug(TAG, "getCurrentLocationData enter");

    let locationChange = (err: BusinessError, location: geoLocationManager.Location): void => {
      if (err) {
        logger.error(TAG, "getCurrentLocationData,locationChanger: err=" + JSON.stringify(err));
        error(err)
      }
      //latitude number 是 否 表示纬度信息，正值表示北纬,负值表示南纬。取值范围为-90到90.
      //longitude number 是 否 表示经度信息，正值表示东经,负值表示西经。取值范围为-180到180.
      //altitude number 是 否 表示高度信息，单位米。
      //accuracy number 是 否 表示精度信息，单位米。
      //speed number 是 否 表示速度信息，单位米每秒。
      //timeStamp number 是 否 表示位置时间戳，UTC格式。
      //direction number 是 否 表示航向信息，单位是“度”，取值范围为0到360。
      //timeSinceBoot number 是 否 表示位置时间戳，开机时间格式。
      //additions Array<string> 是 否 附加信息。
      //additionSize number 是 否 附加信息数量，取值范围为大于等于0。
      //isFromMock Boolean 是 否 表示位置信息是否来自于位置模拟功能，== 系统API：此接口为系统接口。
      if (location) {
        logger.debug(TAG, "getCurrentLocationData,locationChanger,location=" + JSON.stringify(location));
        //todo: 转换obj
        // position: {
        //   coords: {
        //     latitude: number;
        //     longitude: number;
        //     altitude: number | null;
        //     accuracy: number;
        //     altitudeAccuracy: number | null
        //     heading: number | null; // 朝向
        //     speed: number | null;
        //   };
        //   timestamp: number;
        // }
        let position = {
          coords: {
            latitude: location.latitude,
            longitude: location.longitude,
            altitude: location.altitude,
            accuracy: location.accuracy,
            heading: location.direction,
            speed: location.speed,
          },
          timeStamp: location.timeStamp,
        }
        logger.debug(TAG, `getCurrentLocationData,locationChanger,before call success,position=${JSON.stringify(position)}`);
        success(position)
      }
    };

    //export type GeolocationOptions = {
    //  timeout?: number; //求位置 超时时间
    //  maximumAge?: number; //缓存位置多久 单位 ms
    //  enableHighAccuracy?: boolean; //gps or wifi
    let requestInfo: geoLocationManager.CurrentLocationRequest = {
      'priority': geoLocationManager.LocationRequestPriority.FIRST_FIX,
      'scenario': geoLocationManager.LocationRequestScenario.UNSET,
      'maxAccuracy': 0,
      'timeoutMs': 2000 };
    if (options) {
      if (options.timeout) {
        requestInfo.timeoutMs = options.timeout
      }
      if (options.enableHighAccuracy) {
        requestInfo.maxAccuracy = 0
      }
    }

    // 重用上一个loc
    if (options.maximumAge) {
      let lastLoc: geoLocationManager.Location = geoLocationManager.getLastLocation() // 上一次位置
      let now = new Date().getTime()
      logger.debug(TAG, `,getCurrentLocation,now:${now}`);
      logger.debug(TAG, `,getCurrentLocation,lastLoc.timeStamp:${lastLoc.timeStamp}`);
      if ((now - lastLoc.timeStamp) <= options.maximumAge) {
        logger.debug(TAG, ",getCurrentLocation,return lastLoc");
        locationChange(undefined, lastLoc);
        return;
      }
    }
    //获取当前loc
    logger.debug(TAG, ",getCurrentLocation,before call geoLocationManager.getCurrentLocation");
    geoLocationManager.getCurrentLocation(requestInfo, locationChange)
  }

  startObserving(requestInfo): void {
    logger.debug(TAG, ",startObserving enter");
    try {
      logger.debug(TAG, ",startObserving,on second");
      geoLocationManager.on('locationChange', requestInfo, locationChangeListener);
    } catch (error) {
      let err: BusinessError = error as BusinessError;
      logger.error(TAG, `startObserving,startObserving errCode:${err.code},errMessage:${err.message}`);
    }
  }

  stopObserving(): void {
    logger.debug(TAG, ",stopObserving enter");
    try {
      geoLocationManager.off('locationChange', locationChangeListener);
    } catch (error) {
      let err: BusinessError = error as BusinessError;
      logger.error(TAG, `,stopObserving,errCode:${err.code},errMessage:${err.message}`);
    }
  }
}