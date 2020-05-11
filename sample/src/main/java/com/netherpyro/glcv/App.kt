package com.netherpyro.glcv

import android.app.Application

/**
 * @author mmikhailov on 11.05.2020.
 */
class App : Application() {

    companion object {
        lateinit var instance: Application
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
    }
}