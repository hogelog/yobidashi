package org.hogel.yobidashi

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        EventLog.init(this)
    }
}
