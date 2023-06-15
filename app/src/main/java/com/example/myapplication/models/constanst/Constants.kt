package com.example.myapplication.models.constanst

import com.hoho.android.usbserial.BuildConfig

class Constants {
    companion object {
        public val baudRate : List<String> = arrayListOf( "4800", "9600", "14400", "19200", "38400","57600", "115200")
        // values have to be unique within each app

        const val INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB"
        const val INTENT_ACTION_DISCONNECT = BuildConfig.LIBRARY_PACKAGE_NAME + ".Disconnect"
        const val NOTIFICATION_CHANNEL = BuildConfig.LIBRARY_PACKAGE_NAME + ".Channel"
        const val INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.LIBRARY_PACKAGE_NAME+ ".MainActivity"

        // values have to be unique within each app
        const val NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001

    }
}