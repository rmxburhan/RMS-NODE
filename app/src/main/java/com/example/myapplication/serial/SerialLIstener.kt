package com.example.myapplication.serial

import  java.lang.Exception
interface SerialLIstener {
    fun onSerialConnect()
    fun onSerialConnectError(e : Exception)

    fun onSerialRead(data : ByteArray)

    fun onSerialRead(datas : ArrayDeque<ByteArray>)

    fun onIoSerialException(e : java.lang.Exception?)
}