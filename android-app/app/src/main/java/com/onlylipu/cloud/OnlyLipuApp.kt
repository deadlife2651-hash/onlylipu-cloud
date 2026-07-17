package com.onlylipu.cloud

import android.app.Application

class OnlyLipuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OnlyLipuApp
            private set
    }
}
