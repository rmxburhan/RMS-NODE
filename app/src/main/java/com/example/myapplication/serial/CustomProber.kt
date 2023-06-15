package com.example.myapplication.serial

import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialProber

class CustomProber {
    companion object {
        fun getCustomProber() : UsbSerialProber {
            val customTable  = ProbeTable()
            customTable.addProduct(0x1234,0xabcd, FtdiSerialDriver::class.java)
            return UsbSerialProber(customTable)
        }
    }
}