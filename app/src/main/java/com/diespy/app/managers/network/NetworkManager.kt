package com.diespy.app.managers.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat


import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.*
import android.bluetooth.*
import android.os.ParcelUuid
/*
NetworkManager provides a backend for the peer-to-peer connections which allow for local party joining.
Uses Bluetooth Low Energy (BLE) to broadcast available party names allowing for fast and easy connections.

In order to keep the state constant, we store a single instance of a NetworkManager in the PublicNetworkManager
class.
*/
class NetworkManager(private val context: Context) {
    //16b uuid used = 0000B81D-0000-1000-8000-00805F9B34FB
    private val NETWORK_UUID = ParcelUuid.fromString("0000B81D-0000-1000-8000-00805F9B34FB")
    val messageList = mutableListOf<String>()
    private var isAdvertising = false
    //Callback function for detection of a signal: adds unique party names to the messagelist.
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.scanRecord?.serviceData?.get(NETWORK_UUID)?.let { bytes ->
                try {
                    val message = String(bytes, Charsets.UTF_8)

                    if (message !in messageList) {
                        messageList.add(message)
                        Log.d("BLE", "Received message: $message")
                    } else {
                        Log.d("BLE", "Duplicate Message Recieved.")
                    }

                } catch (e: Exception) {
                    Log.e("BLE", "Error decoding message", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Scan failed with error: $errorCode")
        }
    }
    //Advertising callback for checking functionality.
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i("BLE", "Broadcast started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "Broadcast failed: ${errorCodeToString(errorCode)}")
        }
    }


    /*
    broadcast: String --> Boolean
    Broadcasts a message (designed for party name) over the network UUID. Continues indefinitely,
    terminates upon calling stopBroadCast() or application closure.

    Returns status of broadcast starting.
     */
    fun broadcast(message: String): Boolean {
        //Checks lock
        if (isAdvertising) {
            return false
        }

        //Embedds characters in ASCII to maintain 1 byte per character
        val messageBytes = message.toByteArray(Charsets.US_ASCII)
        val maxBytes = 31 // BLE advertising limit (31 bytes)

        //the UUID we use is 16 bytes + 15 for message
        if (messageBytes.size + 16 > maxBytes) {
            Log.e("BLE", "Message $message (${messageBytes}) too long (${messageBytes.size} bytes). Max: $maxBytes bytes")
            return false
        }

        // Get BLE advertiser
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter ?: run {
            Log.e("BLE", "Bluetooth not supported")
            return false
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.e("BLE", "Advertising not supported")
            return false
        }

        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser

        // Advertisement packet consists of the UUID and the party name.
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(NETWORK_UUID)
            .addServiceData(
                NETWORK_UUID,
                messageBytes
            )
            .build()

        //We set connnection settings to maximize range, and to be one-way.
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Start broadcasting
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLE", "Error: Permission BLUETOOTH_ADVERTISE not granted")
            return false
        }
        advertiser.startAdvertising(settings, advertiseData, advertiseCallback)
        isAdvertising = true
        return true
    }
    /*
    stopBroadcast
    Terminates BLE broadcasting. Used when exiting a party.
     */
    fun stopBroadcast() {
        //Checks lock, needed permissions
        if(!isAdvertising) {
            return
        }
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter ?: run {
            Log.e("BLE", "Bluetooth not available")
            return
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.e("BLE", "Advertising not supported")
            return
        }

        try {
            //Stops advertising, opens lock.
            bluetoothAdapter.bluetoothLeAdvertiser?.apply {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("NetworkManager", "Error: Permission BLUETOOTH_ADVERTISE not granted")
                    return
                }
                stopAdvertising(advertiseCallback)
                isAdvertising = false
                Log.d("BLE", "Broadcasting stopped successfully")
            }
        } catch (e: IllegalStateException) {
            Log.e("BLE", "Error stopping broadcast: ${e.message}")
        }
    }

    //Util for error readability visibility
    private fun errorCodeToString(errorCode: Int): String = when (errorCode) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
        else -> "Unknown error"
    }

    /*
    listen()

    For 5 seconds, listens for any messages (partynames) recieved from the networkUUID. If any new
    names are encountered, they're stored in the message list, which can be read by external usrs.
     */
    fun listen() {
        //Sets up adaper
        val bluetoothAdapter = context.getSystemService(Context.BLUETOOTH_SERVICE)?.let {
            it as android.bluetooth.BluetoothManager
        }?.adapter

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            Log.e("BLE", "BLE scanner not available")
            return
        }

        // Scan settings for low latency
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter for our specific service UUID
        val filter = ScanFilter.Builder()
            .setServiceUuid(NETWORK_UUID)
            .build()

        // Start scanning, upon detection refer to the callback function defined at the top of the file.
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLE", "Error, Permission BLUETOOTH_SCAN not granted")// for ActivityCompat#requestPermissions for more details.
            return
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLE", "Error, Permission ACCESS_FINE_LOCATION not granted")
            return
        }
        bleScanner.startScan(listOf(filter), settings, scanCallback)
        Log.d("BLE", "Started listening...")

        // Stop scanning after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("BLE", "Permission Denied: BLUETOOTH_SCAN")
            }


            bleScanner.stopScan(scanCallback)
            Log.d("BLE", "Stopped listening. Total messages: ${messageList.size}")
        }, 5000)
    }
}
