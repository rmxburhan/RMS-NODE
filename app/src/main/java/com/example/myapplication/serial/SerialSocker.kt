package com.example.myapplication.serial

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.lang.Exception
import java.security.InvalidParameterException

class SerialSocker : SerialInputOutputManager.Listener {
    private val WRITE_WAIT_LIMITS = 2000

    private var disconnectBroadcastReceiver : BroadcastReceiver? = null

    private var context : Context? = null
    private var listener: SerialLIstener? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    constructor(
        context: Context,
        connection: UsbDeviceConnection,
        serialPort: UsbSerialPort
    ) {
        if (context is Activity)
            throw InvalidParameterException("expected non UI thread")
        this.context = context
        this.connection = connection
        this.serialPort = serialPort
        disconnectBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                listener?.onIoSerialException(IOException("Background IO disconnected"))
                disconnect()
            }
        }
    }

    fun getName(): String? {
        return serialPort!!.driver.javaClass.simpleName.replace("SerialDriver", "")
    }

    @Throws(IOException::class)
    fun connect(lIstener: SerialLIstener?) {
        this.listener = listener
        context!!.registerReceiver(
            disconnectBroadcastReceiver,
            IntentFilter()
        )
    }

    @Throws(IOException::class)
    fun write(data: ByteArray?) {
        if (serialPort == null) throw IOException("not connected")
        serialPort!!.write(data, WRITE_WAIT_LIMITS)
    }

    fun disconnect() {
        listener = null // ignore remaining data and errors
        if (ioManager != null) {
            ioManager!!.listener = null
            ioManager!!.stop()
            ioManager = null
        }
        if (serialPort != null) {
            try {
                serialPort!!.dtr = false
                serialPort!!.rts = false
            } catch (ignored: Exception) {
            }
            try {
                serialPort!!.close()
            } catch (ignored: Exception) {
            }
            serialPort = null
        }
        if (connection != null) {
            connection!!.close()
            connection = null
        }
        try {
            context!!.unregisterReceiver(disconnectBroadcastReceiver)
        } catch (ignored: Exception) {
        }
    }

    override fun onNewData(data: ByteArray?) {
        if (listener != null) data?.let { listener!!.onSerialRead(it) }
    }

    override fun onRunError(e: Exception?) {
        if (listener != null) listener!!.onIoSerialException(e)
    }
}