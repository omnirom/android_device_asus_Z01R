/*
* Copyright (C) 2017 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.omnirom.device;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.util.Log;
import static android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED;
import android.os.UserHandle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GestureSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String KEY_PROXI_SWITCH = "proxi";
    public static final String KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH = "off_screen_gesture_feedback";

    protected static final String GESTURE_BUF_PATH = "/sys/bus/i2c/devices/3-0038/fts_gesture_buf";
    private static final String GESTURE_MODE_PATH = "/sys/bus/i2c/devices/3-0038/fts_gesture_mode";

    private static String getGestureValue(String value) {
        return value.substring(value.lastIndexOf(":") + 2);
    }

    private static final int KEY_MASK_GESTURE_CONTROL = 0x40;
    public static final int KEY_C_MASK = 0x04; // c gesture mask
    public static final int KEY_E_MASK = 0x08; // e gesture mask
    public static final int KEY_S_MASK = 0x10; // s gesture mask
    public static final int KEY_V_MASK = 0x01; // v gesture mask
    public static final int KEY_W_MASK = 0x20; // w gesture mask
    public static final int KEY_Z_MASK = 0x02; // z gesture mask

    public static final String KEY_C_APP = "c_gesture_app";
    public static final String KEY_E_APP = "e_gesture_app";
    public static final String KEY_V_APP = "v_gesture_app";
    public static final String KEY_S_APP = "s_gesture_app";
    public static final String KEY_W_APP = "w_gesture_app";
    public static final String KEY_Z_APP = "z_gesture_app";
    public static final String KEY_LEFT_SWIPE_APP = "left_gesture_app";
    public static final String KEY_RIGHT_SWIPE_APP = "right_gesture_app";
    public static final String KEY_FP_GESTURE_CATEGORY = "key_fp_gesture_category";
    public static final String KEY_FP_GESTURE_DEFAULT_CATEGORY = "gesture_settings";

    public static final String FP_GESTURE_LONG_PRESS_APP = "fp_long_press_gesture_app";

    public static final String DEVICE_GESTURE_MAPPING_0 = "device_gesture_mapping_0_0";
    public static final String DEVICE_GESTURE_MAPPING_1 = "device_gesture_mapping_1_0";
    public static final String DEVICE_GESTURE_MAPPING_2 = "device_gesture_mapping_2_0";
    public static final String DEVICE_GESTURE_MAPPING_3 = "device_gesture_mapping_3_0";
    public static final String DEVICE_GESTURE_MAPPING_4 = "device_gesture_mapping_4_0";
    public static final String DEVICE_GESTURE_MAPPING_5 = "device_gesture_mapping_5_0";
    public static final String DEVICE_GESTURE_MAPPING_6 = "device_gesture_mapping_6_0";
    public static final String DEVICE_GESTURE_MAPPING_7 = "device_gesture_mapping_7_0";
    public static final String DEVICE_GESTURE_MAPPING_8 = "device_gesture_mapping_8_0";
    public static final String DEVICE_GESTURE_MAPPING_9 = "device_gesture_mapping_9_0";
    public static final String DEVICE_GESTURE_MAPPING_10 = "device_gesture_mapping_10_0";

    private TwoStatePreference mProxiSwitch;
    private TwoStatePreference mFpSwipeDownSwitch;
    private AppSelectListPreference mLetterEGesture;
    private AppSelectListPreference mLetterCGesture;
    private AppSelectListPreference mLetterVGesture;
    private AppSelectListPreference mLetterSGesture;
    private AppSelectListPreference mLetterWGesture;
    private AppSelectListPreference mLetterZGesture;
//    private AppSelectListPreference mLeftSwipeApp;
//    private AppSelectListPreference mRightSwipeApp;
    private AppSelectListPreference mFPDownSwipeApp;
    private AppSelectListPreference mFPUpSwipeApp;
    private AppSelectListPreference mFPRightSwipeApp;
    private AppSelectListPreference mFPLeftSwipeApp;
//    private AppSelectListPreference mFPLongPressApp;
    private PreferenceCategory fpGestures;
    private boolean mFpDownSwipe;
    private List<AppSelectListPreference.PackageItem> mInstalledPackages = new LinkedList<AppSelectListPreference.PackageItem>();
    private PackageManager mPm;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.gesture_settings, rootKey);
        mPm = getContext().getPackageManager();

        mProxiSwitch = (TwoStatePreference) findPreference(KEY_PROXI_SWITCH);
        mProxiSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED, 1) != 0);

        mLetterCGesture = (AppSelectListPreference) findPreference(KEY_C_APP);
        mLetterCGesture.setEnabled(isGestureSupported(KEY_C_APP));
        String value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_0);
        mLetterCGesture.setValue(value);
        mLetterCGesture.setOnPreferenceChangeListener(this);

        mLetterEGesture = (AppSelectListPreference) findPreference(KEY_E_APP);
        mLetterEGesture.setEnabled(isGestureSupported(KEY_E_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1);
        mLetterEGesture.setValue(value);
        mLetterEGesture.setOnPreferenceChangeListener(this);

        mLetterVGesture = (AppSelectListPreference) findPreference(KEY_V_APP);
        mLetterVGesture.setEnabled(isGestureSupported(KEY_V_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2);
        mLetterVGesture.setValue(value);
        mLetterVGesture.setOnPreferenceChangeListener(this);

        mLetterSGesture = (AppSelectListPreference) findPreference(KEY_S_APP);
        mLetterSGesture.setEnabled(isGestureSupported(KEY_S_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3);
        mLetterSGesture.setValue(value);
        mLetterSGesture.setOnPreferenceChangeListener(this);

        mLetterWGesture = (AppSelectListPreference) findPreference(KEY_W_APP);
        mLetterWGesture.setEnabled(isGestureSupported(KEY_W_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_4);
        mLetterWGesture.setValue(value);
        mLetterWGesture.setOnPreferenceChangeListener(this);

        mLetterZGesture = (AppSelectListPreference) findPreference(KEY_Z_APP);
        mLetterZGesture.setEnabled(isGestureSupported(KEY_Z_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_5);
        mLetterZGesture.setValue(value);
        mLetterZGesture.setOnPreferenceChangeListener(this);

//        mLeftSwipeApp = (AppSelectListPreference) findPreference(KEY_LEFT_SWIPE_APP);
//        mLeftSwipeApp.setEnabled(isGestureSupported(KEY_LEFT_SWIPE_APP));
//        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8);
//        mLeftSwipeApp.setValue(value);
//        mLeftSwipeApp.setOnPreferenceChangeListener(this);

//        mRightSwipeApp = (AppSelectListPreference) findPreference(KEY_RIGHT_SWIPE_APP);
//        mRightSwipeApp.setEnabled(isGestureSupported(KEY_RIGHT_SWIPE_APP));
//        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9);
//        mRightSwipeApp.setValue(value);
//        mRightSwipeApp.setOnPreferenceChangeListener(this);

//        mFPLongPressApp = (AppSelectListPreference) findPreference(FP_GESTURE_LONG_PRESS_APP);
//        mFPLongPressApp.setEnabled(true);
//        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10);
//        mFPLongPressApp.setValue(value);
//        mFPLongPressApp.setOnPreferenceChangeListener(this);

        new FetchPackageInformationTask().execute();
    }

    private boolean areSystemNavigationKeysEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(),
               Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0) == 1;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mProxiSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED, mProxiSwitch.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLetterCGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_C_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_0, value);
        } else if (preference == mLetterEGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_E_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1, value);
        } else if (preference == mLetterVGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_V_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2, value);
        } else if (preference == mLetterSGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_S_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3, value);
        } else if (preference == mLetterWGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_W_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_4, value);
        } else if (preference == mLetterZGesture) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_Z_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_5, value);
//        } else if (preference == mLeftSwipeApp) {
//            String value = (String) newValue;
//            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
//            setGestureEnabled(KEY_LEFT_SWIPE_APP, !gestureDisabled);
//            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8, value);
//        } else if (preference == mRightSwipeApp) {
//            String value = (String) newValue;
//            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
//            setGestureEnabled(KEY_RIGHT_SWIPE_APP, !gestureDisabled);
//            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9, value);
//        } else if (preference == mFPLongPressApp) {
//            String value = (String) newValue;
//            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10, value);
        }
        return true;
    }

    public static int getGestureMapping(String key) {
        switch(key) {
            case KEY_C_APP:
                return 0x04;
            case KEY_E_APP:
                return 0x08;
            case KEY_V_APP:
                return 0x01;
            case KEY_S_APP:
                return 0x10;
            case KEY_W_APP:
                return 0x20;
            case KEY_Z_APP:
                return 0x02;
//            case KEY_LEFT_SWIPE_APP:
//                return 0x00;
//            case KEY_RIGHT_SWIPE_APP:
//                return 0x00;

        }
        return 0x00;
    }

    private boolean isGestureSupported(String key) {
        return Utils.fileWritable(GESTURE_BUF_PATH);
    }

    protected void setGestureEnabled(String key, boolean enabled) {
      Log.e("GestureSettings", "setGestureEnabled called with key="+key+", enabled="+enabled);
      String bufLine = Utils.readLine(GESTURE_BUF_PATH);
      String hexBuf = getGestureValue(bufLine);
      int gestureMode = Integer.decode(hexBuf);
      //int mask = ALL_GESTURE_MASKS[gesture.id];
      int mask = getGestureMapping(key);

      if (enabled)
          gestureMode |= mask;
      else
          gestureMode &= ~mask;

      if (gestureMode != 0)
          gestureMode |= KEY_MASK_GESTURE_CONTROL;

      String gestureType = String.format("%7s", Integer.toBinaryString(gestureMode)).replace(' ', '0');
      Log.e("GestureSettings", "gestureType="+gestureType);
      Utils.writeLine(GESTURE_BUF_PATH, gestureType);

      String gestureTypeMapping = Settings.System.getString(getContext().getContentResolver(), Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING);
      Settings.System.putString(getContext().getContentResolver(), Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING, gestureType);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (!(preference instanceof AppSelectListPreference)) {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        DialogFragment fragment =
                AppSelectListPreference.AppSelectListPreferenceDialogFragment
                        .newInstance(preference.getKey());
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager(), "dialog_preference");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFPDownSwipeApp != null) {
            mFPDownSwipeApp.setEnabled(!areSystemNavigationKeysEnabled());
        }
        if (mFPUpSwipeApp != null) {
            mFPUpSwipeApp.setEnabled(!areSystemNavigationKeysEnabled());
        }
    }

    private void loadInstalledPackages() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = mPm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            ActivityInfo activity = info.activityInfo;
            ApplicationInfo appInfo = activity.applicationInfo;
            ComponentName componentName = new ComponentName(appInfo.packageName, activity.name);
            CharSequence label = null;
            try {
                label = activity.loadLabel(mPm);
            } catch (Exception e) {
            }
            if (label != null) {
                final AppSelectListPreference.PackageItem item = new AppSelectListPreference.PackageItem(activity.loadLabel(mPm), 0, componentName);
                mInstalledPackages.add(item);
            }
        }
        Collections.sort(mInstalledPackages);
    }

    private class FetchPackageInformationTask extends AsyncTask<Void, Void, Void> {
        public FetchPackageInformationTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            loadInstalledPackages();
            return null;
        }

        @Override
        protected void onPostExecute(Void feed) {
            mLetterEGesture.setPackageList(mInstalledPackages);
            mLetterCGesture.setPackageList(mInstalledPackages);
            mLetterVGesture.setPackageList(mInstalledPackages);
            mLetterSGesture.setPackageList(mInstalledPackages);
            mLetterWGesture.setPackageList(mInstalledPackages);
            mLetterZGesture.setPackageList(mInstalledPackages);
//            mLeftSwipeApp.setPackageList(mInstalledPackages);
//            mRightSwipeApp.setPackageList(mInstalledPackages);
//            mFPLongPressApp.setPackageList(mInstalledPackages);
        }
    }
}
