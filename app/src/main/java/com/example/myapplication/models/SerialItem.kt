package com.example.myapplication.models

import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.UsbSerialDriver

class SerialItem(
    var usbdevice : UsbDevice,
    var port : Int,
    var driver : UsbSerialDriver?
) {

}