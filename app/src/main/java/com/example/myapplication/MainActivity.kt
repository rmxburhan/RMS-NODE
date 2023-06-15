package com.example.myapplication

import MyCountDownTimer
import android.app.PendingIntent
import android.app.assist.AssistContent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.adapter.CustomListSerialAdapter
import com.example.myapplication.adapter.ListSerialMonitorAdapter
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.models.SerialItem
import com.example.myapplication.models.constanst.Constants
import com.example.myapplication.serial.CustomProber
import com.example.myapplication.ui.GantiIdFragment
import com.example.myapplication.ui.SetTimerDialog
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener, GantiIdFragment.MyDialogListener, SetTimerDialog.SetTimerListener, MyCountDownTimer.timerListener{
    private lateinit var  binding : ActivityMainBinding

    private var serialItemList : ArrayList<ListSerialItem> = ArrayList<ListSerialItem>()
    private var serialList : ArrayList<SerialItem> = ArrayList<SerialItem>()
    private lateinit var serialAdapter : CustomListSerialAdapter
    private var port : UsbSerialPort? = null
    private var usbManger : UsbManager?  = null
    private lateinit var gpsStatusListener : GPSStatusListener
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    // Kode yang dijalankan saat USB terhubung
                    runOnUiThread {
                        refresh()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    // Kode yang dijalankan saat USB terputus
                    runOnUiThread {
                        refresh()
                    }
                }
            }
        }
    }

    private var connected : Connected = Connected.False

    private lateinit var threadWrite : Thread

    private var delay  : Long = 1000

    private var ioManager: SerialInputOutputManager? = null

    private lateinit var locationManager : LocationManager
    private lateinit var locationListener: LocationListener
    enum class Connected {
        True,
        False,
        Pending
    }


    enum class isRunning {
        True, False
    }

    private var runningStatus = isRunning.False

    private var latitude  : Double = 0.0
    private var longitude : Double = 0.0
    private var speed : Float = 0F

    private var listLog : ArrayList<ListSerialItem> = arrayListOf()
    private var idPerangkat = "01"

    private lateinit var timer: MyCountDownTimer
    var oldLatitude : Double = 0.0
    var oldLongitude : Double = 0.0
    data class Coordinate(val latitude: Double, val longitude: Double)
    fun haversineDistance(start: Coordinate, end: Coordinate, radius: Double = 6371.0): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask permissoin
        if (isLocationPermissionGranted())
        {
            setupLocation()
        } else {
            // Request location permission
            requestLocationPermission()
        }


//        // Spinner baud rate
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Constants.baudRate)
//        binding.spinnerBaudRate.adapter = adapter

        // Spinner serial
        serialAdapter = CustomListSerialAdapter(this, serialList)
//        binding.spinner.adapter = serialAdapter

//         Recycler view serial monitor
        binding.listserialMonitor.adapter = ListSerialMonitorAdapter(this, serialItemList)

        // Button refresh
        refresh()
        // Konek port
        binding.btnConnect.setOnClickListener {
            if (connected == Connected.True) {
                disconnect()

            } else {
                connect()
            }
        }
        binding.btnSimpanId.setOnClickListener {
//            port?.write(binding.edtIdPerangkat.text.toString().toByteArray(), 2000)
            idPerangkat = binding.edtIdPerangkat.text.toString()
            Toast.makeText(this, "Berhasil simpan id perangkat", Toast.LENGTH_SHORT).show()
        }

        binding.btnsimpandelay.setOnClickListener {
            delay = binding.edtInputDelay.text.toString().toLong()
            Toast.makeText(this, "Berhasil simpan delay", Toast.LENGTH_SHORT).show()
        }



        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)

        binding.btnGantiId.setOnClickListener {
            showMyDialog()
        }

        binding.btnStart.setOnClickListener {
                    timer = MyCountDownTimer(txtTimer.toString().toLong() * 1000, 1000, this@MainActivity)
            jarak = 0.0
            timerStatus = TimingStatus.True
            timer.start()
        }

        binding.btnStop.setOnClickListener {
            timerStatus = TimingStatus.Pending
            timer.cancel()
        }


        binding.btnSetTimer.setOnClickListener {
            val dialog = SetTimerDialog()
            dialog.show(supportFragmentManager, "MyTimer")
        }


    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                oldLatitude = latitude
                oldLongitude = longitude

                latitude = location.latitude
                longitude = location.longitude
                speed = location.speed * 3.6F

                if (timerStatus == TimingStatus.True) {
                    jarak += haversineDistance(Coordinate(oldLatitude, oldLongitude), Coordinate(latitude, longitude))
                    runOnUiThread {
                        binding.txtJarkditempu.setText("Jarak : ${jarak * 1000} meter")
                    }
                } else if (timerStatus == TimingStatus.Pending) {
                    jarak += haversineDistance(Coordinate(oldLatitude, oldLongitude), Coordinate(latitude, longitude))
                    timerStatus = TimingStatus.False
                    runOnUiThread {
                        binding.txtJarkditempu.setText("Jarak : ${jarak * 1000} meter")
                    }
                }

                binding.txtLat.text = "Latitude : " + latitude.toString()
                binding.txtLng.text = "Longitude : " + longitude.toString()
                binding.txtSog.text = "Speed : " + speed.toString() + " Km/h"
            }
        }
        val context : Context = this@MainActivity
        gpsStatusListener = GPSStatusListener(locationManager, binding.txtStatusGPS)
        if (isLocationPermissionGranted()) {
            locationManager.registerGnssStatusCallback(gpsStatusListener)
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, proceed with location operations
                // ...
                setupLocation()
            } else {
                // Location permission denied, handle accordingly (e.g., show an error message)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private var jarak : Double = 0.0

    private var timerStatus = TimingStatus.False

    enum class TimingStatus {
        True,
        False,
        Pending
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        locationManager.removeUpdates(locationListener)
        disconnect()
    }

    private fun showMyDialog() {
        val dialogFragment = GantiIdFragment()
        dialogFragment.show(supportFragmentManager, "MyDialogFragment")
    }



    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request pembaruan lokasi setiap beberapa detik
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        } else {
            // Jika izin tidak diberikan, minta izin kepada pengguna
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    private fun connect() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty())
        {
            Toast.makeText(this, "Tidak ada device", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedDevice = availableDrivers.get(0).device
        val driver = availableDrivers.get(0)
        port = driver?.ports?.get(0)
        val usbConnection = usbManager.openDevice(selectedDevice)
        if (usbConnection == null) {
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
            val usbPermissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(Constants.INTENT_ACTION_GRANT_USB),
                flags
            )
            usbManager.requestPermission(selectedDevice, usbPermissionIntent)
            return
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(selectedDevice)){
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "open failed", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            port?.open(usbConnection)
            port?.setParameters(115200,UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE )
            ioManager = SerialInputOutputManager(port, this)
            Executors.newSingleThreadExecutor().submit(ioManager)
            Toast.makeText(this, "Usb terkoneksi", Toast.LENGTH_SHORT).show()
            binding.btnConnect.setText("Stop")
            connected = Connected.True
        } catch (ex : Exception) {
            Toast.makeText(this, ex.message , Toast.LENGTH_SHORT).show()
        }
    }


    fun refresh() {
        serialList.clear()
        usbManger = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDefaultProber = UsbSerialProber.getDefaultProber()
        val usbCustomProber = CustomProber.getCustomProber()

        val deviceList1 = usbManger!!.deviceList.values
        for (device in deviceList1) {
            var driver = usbDefaultProber.probeDevice(device)
            if (driver == null) {
                driver = usbCustomProber.probeDevice(device)
            }
            if (driver != null) {
                for (i in 0 .. driver.ports.size - 1) {
                    serialList.add(SerialItem(
                        device,
                        i,
                        driver
                    ))
                }
            } else {
                serialList.add(
                    SerialItem(
                    device,
                    0,
                    null
                )
                )
            }
        }
        binding.btnConnect.isEnabled = serialList.size >= 1
        if (serialList.size > 0) {
            if (port != null) {
                disconnect()
            }
        }
        serialAdapter.notifyDataSetChanged()
    }



    fun disconnect() {
        binding.btnConnect.setText("Start")
        connected = Connected.False
        port?.close()
        port = null
        if (threadWrite != null) {
            threadWrite.interrupt()
        }
    }


    fun write(msg : String) {
        port?.write(msg.toByteArray(), 2000)
        serialItemList.add(
            ListSerialItem(
                SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                msg.trim(),
                R.color.black
            )
        )
        runOnUiThread {
            binding.listserialMonitor.adapter?.notifyDataSetChanged()
        }
    }

    fun writeLine(msg : String) {
        write(msg + "\r\n")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun receive(msg : String) {
        if (msg.trim() == "START,*") {
            serialItemList.add(
                ListSerialItem(
                    SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                    msg,
                    R.color.green
                )
            )
            listLog.clear()
            if (runningStatus == isRunning.False) {
                startWriting()
            }
        }
        else if (msg.trim() == "GO,*") {
            serialItemList.add(
                ListSerialItem(
                    SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                    msg,
                    R.color.red
                )
            )
            if (runningStatus == isRunning.True) {
                timerStatus = TimingStatus.True
                runOnUiThread {
                    binding.txtStatusRunning.setText("Status : Go");

                }
            }
        }
        else if(msg.trim() == "STOP,*") {
            serialItemList.add(
                ListSerialItem(
                    SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                    msg,
                    R.color.red
                )
            )
            if (runningStatus == isRunning.True) {
                stopWriting()
            }
        } else {
            serialItemList.add(
                ListSerialItem(
                    SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                    msg,
                    R.color.black
                )
            )
        }
        if (runningStatus == isRunning.True) {
            listLog.add(
                ListSerialItem(
                    SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                    msg,
                    R.color.black
                )
            )
        }
        runOnUiThread {
            binding.listserialMonitor.adapter?.notifyDataSetChanged()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
    }

    private var receivedDataBuffer = StringBuilder()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewData(data: ByteArray) {
        val receivedData = String(data, Charset.defaultCharset())

        // Append the received data to the buffer
        receivedDataBuffer.append(receivedData)

        // Process complete lines
        var lineEndIndex = receivedDataBuffer.indexOf("\r\n")
        if (lineEndIndex == -1) {
            // Check for '\r' only
            lineEndIndex = receivedDataBuffer.indexOf('\r')
        }
        if (lineEndIndex == -1) {
            // Check for '\n' only
            lineEndIndex = receivedDataBuffer.indexOf('\n')
        }

        while (lineEndIndex != -1) {
            // Extract the complete line from the buffer
            val line = receivedDataBuffer.substring(0, lineEndIndex).trim()

            // Process the complete line
            if (!line.isNullOrEmpty()) {
                receive(line)
            }

            // Appen
            // Remove the processed line from the buffer
            receivedDataBuffer.delete(0, lineEndIndex + 1)

            // Check for more complete lines
            lineEndIndex = receivedDataBuffer.indexOf("\r\n")
            if (lineEndIndex == -1) {
                // Check for '\r' only
                lineEndIndex = receivedDataBuffer.indexOf('\r')
            }
            if (lineEndIndex == -1) {
                // Check for '\n' only
                lineEndIndex = receivedDataBuffer.indexOf('\n')
            }
        }
    }

    private var jarakGo : Double = 0.0
    override fun onRunError(e: java.lang.Exception) {
        e.printStackTrace()
    }
    private var oldLatitudeTemp : Double? = null
    private var oldLongitudeTemp : Double? = null
    private var latNow : Double? = null
    private var lngNow : Double? = null
    private fun startWriting() {
        runningStatus = isRunning.True
        runOnUiThread {
            binding.txtJarkditempu.setText("0 meter")
            binding.txtStatusRunning.text = "Status : Bersiap"
        }
        jarak = 0.0
        threadWrite = Thread {
            while(runningStatus == isRunning.True) {
                val batteryStatus= IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    applicationContext.registerReceiver(null, ifilter)
                }
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct: Float = level / scale.toFloat() * 100
                if (oldLatitudeTemp == null) {
                    oldLatitudeTemp = latitude
                    oldLongitudeTemp = longitude
                    latNow = latitude
                    lngNow = longitude
                } else {
                    oldLatitudeTemp = latNow
                    oldLongitudeTemp = lngNow
                    latNow = latitude
                    lngNow = longitude
                }
                val message = "MKRRMP${idPerangkat}1222,${latitude},${longitude},${speed},${batteryPct.toInt()},*"
                writeLine(message)
                if ((oldLatitudeTemp != null && oldLongitudeTemp != null && latNow != null && lngNow != null) && timerStatus == TimingStatus.True) {
                    jarakGo += haversineDistance(Coordinate(oldLatitudeTemp!!, oldLongitudeTemp!!),
                        Coordinate(latNow!!, lngNow!!))
                    runOnUiThread {
                        binding.txtJarakGO.setText( "Jarak : ${jarakGo * 1000} meter")
                    }
                }
                listLog.add(
                    ListSerialItem(
                        SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                        message,
                        R.color.black
                    )
                )
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
            }
        }
        threadWrite.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopWriting() {
        try {
            timerStatus = TimingStatus.Pending
            runOnUiThread {
                binding.txtStatusRunning.text = "Status : Idle"
                binding.txtJarakGO.setText("Jarak : 0 meter")
            }
            runningStatus = isRunning.False
            threadWrite.interrupt()
        } catch (ex : java.lang.Exception) {
            ex.printStackTrace()
        }
        val batteryStatus= IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Float = level / scale.toFloat() * 100
        val message = "MKRRMP${idPerangkat}1222,${latitude},${longitude},${speed},${batteryPct},*"
        writeLine(message)
        jarakGo += haversineDistance(Coordinate(latNow!!, lngNow!!),
            Coordinate(latitude, longitude))
        runOnUiThread {
            binding.txtJarakGO.setText("Jarak : ${jarakGo * 1000} meter")
        }
        serialItemList.add(
            ListSerialItem(
                SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                message,
                R.color.black
            )
        )
        listLog.add(
            ListSerialItem(
                SimpleDateFormat("hh:mm:ss").format(Date()).toString(),
                message,
                R.color.black
            )
        )
        var text = ""
        for (i in listLog) {
            text += "${i.data} ${i.time} \n"
        }
        var filePath = saveTextToFile(this@MainActivity, text, LocalDateTime.now().toString())
        runOnUiThread {
            Toast.makeText(this, "File raw data saved to : ${filePath}", Toast.LENGTH_SHORT).show()
        }
    }

    private  var lastLocation : Location? = null
    fun saveTextToFile(context: Context, text: String, fileName: String): String? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
        }

        val resolver = context.contentResolver
        var outputStream: OutputStream? = null
        var uri: Uri? = null
        var filePath: String? = null

        try {
            val collection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

            uri = resolver.insert(collection, values)
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
                outputStream?.write(text.toByteArray())
                filePath = uriToFilePath(context, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
        }
        return filePath
    }

    fun uriToFilePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val filePath = if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            cursor.getString(columnIndex)
        } else {
            null
        }
        cursor?.close()
        return filePath
    }

    override fun onDialogPositiveClick(inputData: String) {
        idPerangkat = "${inputData}"
        binding.txtIdPerangkat.text = "ID perangkat : MKRRMP${idPerangkat}1222"
    }
    var txtTimer = ""
    override fun onSaveTimer(inputData: String) {
        txtTimer = inputData
        binding.txtTarget.setText("Target waktu : ${txtTimer.toInt()} detik")
    }

    override fun onTick(inputData: String) {
        binding.txtTimer.setText("${inputData} Detik")
    }

    override fun onFinish() {
        timerStatus = TimingStatus.Pending
        runOnUiThread {
            binding.txtTimer.setText("00 Detik")
        }
    }

}

class GPSStatusListener(private val locationManager: LocationManager, private val txtStatusGps : TextView) :
    GnssStatus.Callback() {

    override fun onStarted() {
        super.onStarted()
        // GPS signal started
        println("GPS signal started")
    }

    override fun onStopped() {
        super.onStopped()
        // GPS signal stopped
        println("GPS signal stopped")
    }

    override fun onFirstFix(ttffMillis: Int) {
        super.onFirstFix(ttffMillis)
        // First GPS fix obtained
        println("First GPS fix obtained")
    }

    override fun onSatelliteStatusChanged(status: GnssStatus) {
        super.onSatelliteStatusChanged(status)
        // Check GPS signal strength
        val satelliteCount = status.satelliteCount
        var strongSignalCount = 0
        var weakSignalCount = 0
        var noSignalCount = 0

        for (i in 0 until satelliteCount) {
            val signalStrength = status.getCn0DbHz(i)
            if (signalStrength >= 30) {
                // Strong signal
                txtStatusGps.setText("Status GPS : Strong")
            } else if (signalStrength >= 20) {
                // Weak signal
                txtStatusGps.setText("Status GPS : Weak")
            } else {
                // No signal
                txtStatusGps.setText("Status GPS : No signal")
            }
        }
    }



}