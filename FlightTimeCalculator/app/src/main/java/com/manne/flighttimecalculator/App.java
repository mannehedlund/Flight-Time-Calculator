package com.manne.flighttimecalculator;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

/**
 * An Application subclass which ensures that timezone information is
 * initialised using the ThreeTen Android Backport.
 */
public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}