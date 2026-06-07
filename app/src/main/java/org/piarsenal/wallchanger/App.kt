package org.piarsenal.wallchanger

import android.app.Application
import org.piarsenal.wallchanger.work.Notifications

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        // WorkManager uses its default initializer (declared in its manifest) and restores
        // scheduled periodic work across reboots on its own, so nothing is wired up here.
    }
}
