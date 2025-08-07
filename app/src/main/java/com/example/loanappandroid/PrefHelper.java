package com.example.loanappandroid;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefHelper {

    private static PrefHelper prefHelper;
    private final SharedPreferences mPrefs;


    private final String PREF_IS_DEVICE_UNIQUE_ID = "PREF_IS_DEVICE_UNIQUE_ID";




    private PrefHelper(Context context) {
        mPrefs = context.getSharedPreferences("mtb_pref", Context.MODE_PRIVATE);
    }

    public static PrefHelper getInstance(Context context) {
        if (prefHelper == null) {
            return new PrefHelper(context);
        } else {
            return prefHelper;
        }
    }


    public void prefSetIsDeviceUniqueId(String devId) {
        mPrefs.edit().putString(PREF_IS_DEVICE_UNIQUE_ID,devId).apply();
    }

    public String prefGetIsDeviceUniqueId() {
        return mPrefs.getString(PREF_IS_DEVICE_UNIQUE_ID, "");
    }
    //new



}
