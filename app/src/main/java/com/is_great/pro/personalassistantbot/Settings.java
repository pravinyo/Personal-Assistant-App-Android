package com.is_great.pro.personalassistantbot;


import android.content.SharedPreferences;
import android.os.Bundle;

import android.preference.PreferenceManager;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;


/**
 * Created by Pravinyo on 3/29/2017.
 */

public class Settings extends AppCompatActivity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isNightMode = preference.getBoolean(getString(R.string.settings_preference_key),false);
        if(isNightMode){
            setTheme(R.style.PAB);
        }else{
            setTheme(R.style.MainTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
