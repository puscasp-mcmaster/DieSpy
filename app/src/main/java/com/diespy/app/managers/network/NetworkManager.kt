import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.home.PartyItem
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface

/*
------------------------------------------------
THIS IS NOT THE FRONTFACING CLASS! FOR THAT SEE PublicNetworkManager.kt!!!!
THIS IS THE BACKEND NETWORK MANAGER CODE!


two distinct steps: 1) connection to a device via wifip2p to obtain local ip,
2) tcp connection to local ip to send data.

ALWAYS USE initAsHost(), initAsClient(), sendHostMessage(), sendClientMessage() and getMessage()!


-PP


 */
class NetworkManager(private val context: Context) {
    private val wifiP2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    private val channel: WifiP2pManager.Channel? = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
    private var peerReceiver: BroadcastReceiver? = null
    private var discoveryCallback: ((List<WifiP2pDevice>) -> Unit)? = null
    private val connectedClients = mutableListOf<Socket>()
    private var serverSocket: ServerSocket? = null
    private var hostAddress: String? = null
    val discoveredDeviceMap = mutableMapOf<String, PartyItem>()
  //  val discoveredDeviceMap = mutableMapOf<String, Triple<String, Int, String>>()
    var latestMessage: String? = null

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    // Skip loopback and IPv6 addresses
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error getting local IP: ${e.message}")
        }
        return null
    }
/*
    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val context = this.context.applicationContext
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(context, permissions.toTypedArray(), 1)
            } else {
                Log.e("NetworkManager", "Permissions cannot be requested without an Activity context")
            }
        }
    }
*/
    private fun advertiseService(groupName: String, groupId: String) {
            val record = hashMapOf(
                "service_name" to "DiespyApp",
                "ip" to getLocalIpAddress(),
                "port" to "5675",
                "groupName" to groupName,
                "groupID" to groupId
            )
            val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("DiespyService", "_diespy._tcp", record)

            //whines about possibly missing permissions, leave for now since it works.
            wifiP2pManager?.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    //todo host success
                }

                override fun onFailure(code: Int) {
                    //todo host fail
                }
            })
        }

    //MAKE PRIVATE LATER
    public fun discoverServices(callback: (List<WifiP2pDevice>) -> Unit) {
        discoveryCallback = callback
        Log.d("NetworkManager", "Callback is ${if (discoveryCallback != null) "set: ${discoveryCallback}" else "null"}")
       // requestPermissionsIfNeeded()
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        for (permission in permissions) {
            val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("NetworkManager", "$permission granted: $isGranted")
        }

        //ServiceListener for callbacks
        val serviceListener = WifiP2pManager.DnsSdServiceResponseListener { instanceName, _, srcDevice ->
            Log.d("NetworkManager", "DnsSdServiceResponseListener triggered")
            if (instanceName == "DiespyService") {
                Log.d("NetworkManager", "Found Diespy device: ${srcDevice.deviceName}")
                discoveryCallback?.invoke(listOf(srcDevice))
            } else {
                Log.d("NetworkManager", "Service found, but not  DiespyService: $instanceName")
            }
        }

        //TextListener to actually read the ip/port/info:
        val txtRecordListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomainName, txtRecordMap, srcDevice ->
            Log.d("NetworkManager", "TXT Record received: $txtRecordMap")
            val hostIp = txtRecordMap["ip"] as String ?: "N/A"
            val hostPort = txtRecordMap["port"] as String ?: "5675"
            val groupName = txtRecordMap["groupName"] as String ?: "Unnamed Group"
            val groupId = txtRecordMap["groupName"] as String ?: "N/A"
            srcDevice.deviceAddress?.let { deviceAddress ->
                discoveredDeviceMap[deviceAddress] = PartyItem(groupId, groupName, -1, hostIp, hostPort)
                //discoveredDeviceMap[deviceAddress] = Triple(hostIp, hostPort.toInt(), groupName)
            }
        }

        wifiP2pManager?.setDnsSdResponseListeners(channel, serviceListener, null)
        Log.d("NetworkManager", "Ready to discover services!")
        Log.d("NetworkManager", "wifiP2pManager is ${if (wifiP2pManager != null) "initialized" else "null"}")
        Log.d("NetworkManager", "channel is ${if (channel != null) "initialized" else "null"}")
        // Stop peer discovery
        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("NetworkManager", "Peer discovery stopped successfully.")

                // Stop service discovery
                wifiP2pManager?.clearServiceRequests(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d("NetworkManager", "Service discovery stopped successfully.")
                    }

                    override fun onFailure(code: Int) {
                        Log.e("NetworkManager", "Failed to stop service discovery: $code")
                    }
                })
            }
            override fun onFailure(code: Int) {
                Log.e("NetworkManager", "Failed to stop peer discovery: $code")
            }
        })


        //Discover peers, if successful discover services.
        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("NetworkManager", "Peer discovery started successfully.")
                //Starts a handler to search for peers in the background, after 10 loops it forces a callback with the empty list
                Handler(Looper.getMainLooper()).postDelayed({
                    //Asynchronously requests peers. Internal logic (peerlist ->) runs only upon completion.

                wifiP2pManager?.requestPeers(channel) { peerList ->
                    //service discovery!
                    if (peerList.deviceList.isNotEmpty()) {
                        Log.d("NetworkManager", "Peers discovered: ${peerList.deviceList.size}")
                        wifiP2pManager?.discoverServices(channel, object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                Log.d("NetworkManager", "Service discovery started successfully.")
                            }

                            override fun onFailure(code: Int) {
                                Log.e("NetworkManager", "Service discovery failed: $code")
                            }
                        })
                    } else {
                        //forces return of empty list after 10 seconds of handler not found.
                        Log.e("NetworkManager", "No peers found.")
                        discoveryCallback?.invoke(emptyList())
                    }
                }
                }, 10000) // 10 second search period to find devices and services. Most sources say it should take < 5, so double jic.
            }

            override fun onFailure(code: Int) {
                Log.e("NetworkManager", "Peer discovery failed: $code")
            }
        })
    }



    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //on connection setup clientside port and connect
                Log.d("NetworkManager", "Connection initiated successfully")
            }

            override fun onFailure(code: Int) {
                //on failure exit
                Log.e("NetworkManager", "Connection failed: $code")
            }
        })
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(5675)
                Log.d("NetworkManager", "Server started. Waiting for connections...")

                while (true) {
                    val clientSocket = serverSocket!!.accept()  // Accept client connection
                    connectedClients.add(clientSocket)
                    Log.d("NetworkManager", "Client connected: ${clientSocket.inetAddress.hostAddress}")
                    // Start a thread for each client
                    Thread {
                        handleClientMessages(clientSocket)
                    }.start()
                }
            } catch (e: Exception) {
                Log.e("NetworkManager", "Server error: ${e}")
            }
        }.start()
    }

    private fun handleClientMessages(clientSocket: Socket) {
        try {
            val inputStream = clientSocket.getInputStream()
            val outputStream = clientSocket.getOutputStream()

            val reader = inputStream.bufferedReader()
            val writer = outputStream.bufferedWriter()

            while (true) {
                val messageFromClient = reader.readLine()  // Read from client
                if (messageFromClient == null) break
                latestMessage = messageFromClient
                Log.d("NetworkManager", "Received from client: $messageFromClient")

                // Send a response to the client
                val response = "Server received: $messageFromClient"
                writer.write("$response\n")
                writer.flush()
                Log.d("NetworkManager", "Response sent to client: $response")
            }

            connectedClients.remove(clientSocket)
            clientSocket.close()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error handling client: ${e}")
        }
    }

    public fun openClientSocket(partyToJoin: PartyItem) {
        //hostAddress = "127.0.0.1"
        hostAddress = partyToJoin.ipAddress
        if (hostAddress == null) {
            Log.e("NetworkManager", "Device IP not available. Cannot open socket.")
            return
        }
        Thread {
            try {
                val socket = Socket(hostAddress, 5675)
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                val reader = inputStream.bufferedReader()
                val writer = outputStream.bufferedWriter()

                // Send initial message
                writer.write("Connection Successful")
                writer.flush()
                Log.d("NetworkManager", "Connection established at: $hostAddress")

                while (true) {
                    val responseFromServer = reader.readLine()  // Read response from server
                    if (responseFromServer == null) break
                    latestMessage = responseFromServer
                    Log.d("NetworkManager", "Response from server: $responseFromServer")

                }

                socket.close()
            } catch (e: Exception) {
                Log.e("NetworkManager", "Error sending/receiving message: ${e}")
            }
        }.start()
    }

    //PUBLIC BELOW HERE
    public fun sendMessageToClients(message: String) {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            Log.d("NetworkManager", "Sending message to all connected clients: $message")
            Log.d("NetworkManager", connectedClients.toString())
            for (client in connectedClients) {
               Thread {
                Log.d(
                    "NetworkManager",
                    "Sending message to client: ${client}. Socket status: ${(client.toString())}"
                )

                try {
                    val outputStream = client.getOutputStream()
                    Log.d("NetworkManager", "OutputStream initialized: $outputStream")
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
                    Log.d("NetworkManager", "BufferedWriter initialized")
                    writer.write("$message\n")
                    Log.d("NetworkManager", "Message written")
                    Thread.sleep(50)
                    writer.flush()
                    Log.d("NetworkManager", "Writer flushed")
                } catch (e: Exception) {
                    Log.e(
                        "NetworkManager",
                        "Error sending message to client ${client}: Exception '${e}'"
                    )
                }
            }.start()
        }
        } else {
            Log.e("NetworkManager", "Server socket is not open. Cannot send message.")
        }
    }

    public fun sendMessageToHost(message: String) {
        Thread {
            try {
                Log.d("NetworkManager", "Attempting to send message: $message")

                // Connect to the host’s socket
                val socket = Socket(hostAddress, 5675)
                val outputStream = socket.getOutputStream()
                val writer = PrintWriter(outputStream, true)

                // Send the message
                writer.println(message)
                writer.flush()

                Log.d("NetworkManager", "Message sent successfully!")

                // Close resources
                writer.close()
                socket.close()
            } catch (e: Exception) {
                Log.e("NetworkManager", "Error sending message: ${e.message}")
            }
        }.start()
    }

    public fun initAsHost(context: Context) {
        val partyName : String = SharedPrefManager.getCurrentPartyName(context) ?: "Unnamed Party"
        val partyId :String = SharedPrefManager.getCurrentPartyId(context) ?: "N/A"
        advertiseService(partyName, partyId)
        startServer()
    }
    public fun getMessage(): String {
        if(latestMessage.isNullOrEmpty())
            return ""
        else
            return latestMessage!!
    }

}
