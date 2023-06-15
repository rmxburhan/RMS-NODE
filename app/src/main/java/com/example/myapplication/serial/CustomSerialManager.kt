package com.example.myapplication.serial

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialPort.Parity

class CustomSerialManager {
    lateinit var usbSerialPort : UsbSerialPort
    lateinit var usbManager: UsbManager
    lateinit var driver : UsbSerialDriver

    fun openConnectoin(device : UsbDevice, baudRate : Int) {

    }
}