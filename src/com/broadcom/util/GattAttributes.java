/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.broadcom.util;

import java.util.UUID;

public class GattAttributes {
    /** Descriptor used to enable/disable notifications/indications */
    public static final UUID CLIENT_CONFIG_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID SENSOR_SERVICE_UUID = UUID
            .fromString("739298B6-87B6-4984-A5DC-BDC18B068985");
    public static final UUID SENSOR_NOTIFICATION_UUID = UUID
            .fromString("33EF9113-3B55-413E-B553-FEA1EAADA459");

    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");
}
