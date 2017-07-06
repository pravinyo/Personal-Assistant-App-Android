package com.is_great.pro.personalassistantbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Pravinyo on 3/30/2017.
 */

public class SettingsFragment extends PreferenceFragment
{
    private static final String LOG_TAG ="settingsFragments";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
//        sharedPrefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public  void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//                boolean test = sharedPreferences.getBoolean(getString(R.string.settings_preference_key), false);
//                Log.e(LOG_TAG, "Value:" + test);
//                if(test){
//                    Toast.makeText(getActivity(),"Night Mode is Activated go back",Toast.LENGTH_SHORT).show();
//                }else {
//                    Toast.makeText(getActivity(),"Day Mode is Activated go back",Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

    }


}