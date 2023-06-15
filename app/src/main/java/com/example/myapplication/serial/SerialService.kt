package com.example.myapplication.serial

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.models.constanst.Constants
import java.io.IOException
import kotlin.reflect.KClass
import  java.lang.Exception
class SerialService : Service(), SerialLIstener  {

    class SerialBinder : Binder() {
        fun getService() : KClass<SerialService> {return SerialService::class}
    }

    private enum class QueueType {
        Connect,
        ConnectError,
        Read,
        IOError
    }

    companion object {
        private class QueueItem {
            lateinit var queueType : QueueType
            lateinit var datas : ArrayDeque<ByteArray>
            lateinit var e : Exception

            constructor(queueType: QueueType) {
                this.queueType = queueType
                if (queueType == QueueType.Read) {
                    init()
                }
            }

            constructor(type : QueueType, e: Exception) {
                this.queueType = type
                this.e = e
            }

            constructor(type : QueueType, datas : ArrayDeque<ByteArray>) {
                this.queueType = type
                this.datas = datas
            }

            fun init() {
                datas = ArrayDeque()
            }

            fun add(data : ByteArray) {
                datas.add(data)
            }
        }
    }

    private lateinit var mainLooper: Handler
    private lateinit var binder: IBinder
    private lateinit var queue1: ArrayDeque<QueueItem>
    private lateinit var queue2: ArrayDeque<QueueItem>
    private lateinit var lastRead : QueueItem
    private var socket: SerialSocker? = null
    private var listener: SerialLIstener? = null
    private var connected : Boolean = false

    fun SerialService() {
        mainLooper = Handler(Looper.getMainLooper())
        binder = SerialBinder()
        queue1 = ArrayDeque()
        queue2 = ArrayDeque()
        lastRead = QueueItem(QueueType.Read)
    }

    private fun cancelNotification() {
        stopForeground(true)
    }
    override fun onDestroy() {
        cancelNotification()
        disconnect()
        super.onDestroy()
    }

    fun disconnect() {
        connected = false // ignore data,errors while disconnecting
        cancelNotification()
        if (socket != null) {
            socket!!.disconnect()
            socket = null
        }
    }

    @Throws(IOException::class)
    fun connect(socket: SerialSocker) {
        socket.connect(this)
        this.socket = socket
        connected = true
    }

    override fun onSerialConnect() {
        if (connected) {
            synchronized(this) {
                if (listener != null) {
                    mainLooper!!.post {
                        if (listener != null) {
                            listener!!.onSerialConnect()
                        } else {
                            queue1!!.add(
                                QueueItem(
                                    QueueType.Connect
                                )
                            )
                        }
                    }
                } else {
                    queue2!!.add(QueueItem(QueueType.Connect))
                }
            }
        }
    }

    @Throws(IOException::class)
    fun write(data : ByteArray) {
        if (!connected)
            throw IOException("not connected")
        socket?.write(data)
    }

    fun attach(serialLIstener: SerialLIstener) {
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            throw IllegalArgumentException("not in main thread")
        }
        cancelNotification()

        synchronized(this) {
            this.listener = listener
        }

        for (item in queue1!!) {
            when (item.queueType) {
                QueueType.Connect -> listener!!.onSerialConnect()
                QueueType.ConnectError -> listener!!.onSerialConnectError(item.e)
                QueueType.Read -> listener?.onSerialRead(item.datas)
                QueueType.IOError -> listener?.onIoSerialException(item.e)
            }
        }

        for (item in queue2!!) {
            when (item.queueType) {
                QueueType.Connect -> listener!!.onSerialConnect()
                QueueType.ConnectError -> listener!!.onSerialConnectError(item.e)
                QueueType.Read -> listener?.onSerialRead(item.datas)
                QueueType.IOError -> listener?.onIoSerialException(item.e)
            }
        }
        queue1!!.clear()
        queue2!!.clear()
    }

    fun detach() {
        if (connected)
            createNotification()
        listener = null
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nc = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL,
                "Background service",
                NotificationManager.IMPORTANCE_LOW
            )
            nc.setShowBadge(false)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(nc)
        }
        val disconnectIntent = Intent()
            .setAction(Constants.INTENT_ACTION_DISCONNECT)
        val restartIntent = Intent()
            .setClassName(this, Constants.INTENT_CLASS_MAIN_ACTIVITY)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val disconnectPendingIntent = PendingIntent.getBroadcast(this, 1, disconnectIntent, flags)
        val restartPendingIntent = PendingIntent.getActivity(this, 1, restartIntent, flags)
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(resources.getColor(R.color.primary))
                .setContentTitle(resources.getString(R.string.app_name))
                .setContentText(if (socket != null) "Connected to " + socket!!.getName() else "Background Service")
                .setContentIntent(restartPendingIntent)
                .setOngoing(true)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_clear_white,
                        "Disconnect",
                        disconnectPendingIntent
                    )
                )
        // @drawable/ic_notification created with Android Studio -> New -> Image Asset using @color/colorPrimaryDark as background color
        // Android < API 21 does not support vectorDrawables in notifications, so both drawables used here, are created as .png instead of .xml
        val notification = builder.build()
        startForeground(Constants.NOTIFY_MANAGER_START_FOREGROUND_SERVICE, notification)
    }

    override fun onSerialConnectError(e: Exception) {
        if (connected) {
            synchronized(this) {
                if (listener != null) {
                    mainLooper!!.post {
                        if (listener != null) {
                            listener!!.onSerialConnectError(e)
                        } else {
                            queue1!!.add(
                                QueueItem(
                                    QueueType.ConnectError,
                                    e
                                )
                            )
                            disconnect()
                        }
                    }
                } else {
                    queue2!!.add(
                        QueueItem(
                            QueueType.ConnectError,
                            e
                        )
                    )
                    disconnect()
                }
            }
        }
    }

    override fun onSerialRead(data: ByteArray) {

        if (connected) {
            synchronized(this) {
                if (listener != null) {
                    var first: Boolean
                    synchronized(lastRead!!) {
                        first = lastRead!!.datas.isEmpty() // (1)
                        if (data != null) {
                            lastRead!!.add(data)
                        } // (3)
                    }
                    if (first) {
                        mainLooper!!.post {
                            var datas: ArrayDeque<ByteArray>
                            synchronized(lastRead!!) {
                                datas = lastRead!!.datas
                                lastRead!!.init() // (2)
                            }
                            listener?.onSerialRead(datas)
                                ?: queue1!!.add(
                                    QueueItem(
                                        QueueType.Read,
                                        datas
                                    )
                                )
                        }
                    }
                } else {
                    if (queue2!!.isEmpty() || queue2!!.last().queueType != QueueType.Read) queue2!!.add(
                        QueueItem(
                            QueueType.Read
                        )
                    )
                    queue2!!.last().add(data)
                }
            }
        }
    }

    override fun onSerialRead(datas: ArrayDeque<ByteArray>) {
        throw UnsupportedOperationException()
}

    override fun onIoSerialException(e: Exception?) {
        if (connected) {
            synchronized(this) {
                if (listener != null) {
                    mainLooper.post {
                        if (listener != null) {
                            listener!!.onIoSerialException(e)
                        } else {
                            if (e != null) {

                            queue1.add(
                                QueueItem(
                                    QueueType.IOError,
                                    e
                                )
                            )
                            }

                            disconnect()
                        }
                    }
                } else {
                    if (e != null) {

                    queue2.add(
                        QueueItem(
                            QueueType.IOError,
                            e
                        )
                    )
                    }
                    disconnect()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}