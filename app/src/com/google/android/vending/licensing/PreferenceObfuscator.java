/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.google.android.vending.licensing;

import android.content.SharedPreferences;
import android.util.Log;
import org.joda.time.DateTime;

/**
 * An wrapper for SharedPreferences that transparently performs data obfuscation.
 */
public class PreferenceObfuscator {

    private static final String TAG = "PreferenceObfuscator";

    private final SharedPreferences mPreferences;
    private final Obfuscator mObfuscator;
    private SharedPreferences.Editor mEditor;

    /**
     * Constructor.
     *
     * @param sp A SharedPreferences instance provided by the system.
     * @param o The Obfuscator to use when reading or writing data.
     */
    public PreferenceObfuscator(SharedPreferences sp, Obfuscator o) {
        mPreferences = sp;
        mObfuscator = o;
        mEditor = null;
    }

    public void putString(String key, String value) {
        if (mEditor == null) {
            mEditor = mPreferences.edit();
        }
        String obfuscatedValue = mObfuscator.obfuscate(value, key);
        try {
            mEditor.putString(key, obfuscatedValue);
        } catch (NullPointerException ex) {
            Log.e(TAG, "ignoring NULL editor", ex);
        }
    }

    public String getString(String key, String defValue) {
        String result;
        String value = mPreferences.getString(key, null);
        if (value != null) {
            try {
                result = mObfuscator.unobfuscate(value, key);
            } catch (ValidationException e) {
                // Unable to unobfuscate, data corrupt or tampered
                Log.w(TAG, "Validation error while reading preference: " + key);
                result = defValue;
            }
        } else {
            // Preference not found
            result = defValue;
        }
        return result;
    }

    public void putLong(String key, long value) {
        putString(key, Long.toString(value));
    }

    public long getLong(String key, long defValue) {
        String value = getString(key, Long.toString(defValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defValue;
        }
    }

    public void putDateTime(String key, DateTime value) {
        putLong(key, value.getMillis());
    }

    public DateTime getDateTime(String key, DateTime defValue) {
        long unixTimestamp = getLong(key, 0);
        return unixTimestamp != 0 ? new DateTime(unixTimestamp) : defValue;
    }

    public void commit() {
        if (mEditor != null) {
            mEditor.apply();
            mEditor = null;
        }
    }
}
