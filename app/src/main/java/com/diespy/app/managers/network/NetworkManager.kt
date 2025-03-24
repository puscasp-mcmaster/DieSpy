import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.io.BufferedWriter
import java.io.OutputStreamWriter
/*
------------------------------------------------
THIS IS NOT THE FRONTFACING CLASS! FOR THAT SEE PublicNetworkManager.kt!!!!
THIS IS THE BACKEND NETWORK MANAGER CODE!


two distinct steps: 1) connection to a device via wifip2p to obtain local ip,
2) tcp connection to local ip to send data.

ALWAYS USE initAsHost(), initAsClient(), sendHostMessage(), sendClientMessage() and getMessage()!


-PP


 */
class NetworkManager(private val context: Context) {/*
    private val wifiP2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    private val channel: WifiP2pManager.Channel? = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
    private var peerReceiver: BroadcastReceiver? = null
    private var discoveryCallback: ((List<WifiP2pDevice>) -> Unit)? = null
    private val connectedClients = mutableListOf<Socket>()
    private var serverSocket: ServerSocket? = null
    var latestMessage: String? = null

    private fun requestPermissions(activity: Activity) {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (permissions.any { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), 1)
        }
    }

    private fun advertiseService() {
        val record = hashMapOf("service_name" to "DiespyApp")
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

        val serviceListener = WifiP2pManager.DnsSdServiceResponseListener { instanceName, _, srcDevice ->
            if (instanceName == "DiespyService") {
                Log.d("NetworkManager", "Found Diespy device: ${srcDevice.deviceName}")
                discoveryCallback?.invoke(listOf(srcDevice))
            }
        }

        wifiP2pManager?.setDnsSdResponseListeners(channel, serviceListener, null)

        wifiP2pManager?.discoverServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //todo client fail
            }
            override fun onFailure(code: Int) {
                //todo client success
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
      //  Thread {
            try {
                serverSocket = ServerSocket(8988)
                Log.d("NetworkManager", "Server started. Waiting for connections...")

                while (true) {
                    val clientSocket = serverSocket!!.accept()  // Accept client connection
                    connectedClients.add(clientSocket)
                    Log.d("NetworkManager", "Client connected: ${clientSocket.inetAddress.hostAddress}")

                    // Start a thread for each client
                  //  Thread {
                        handleClientMessages(clientSocket)
                 //   }.start()
                }
            } catch (e: Exception) {
                Log.e("NetworkManager", "Server error: ${e}")
            }
     //   }.start()
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

    private fun openClientSocket(hostAddress: String) {
     //   Thread {
            try {
                val socket = Socket(hostAddress, 8988)
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
       // }.start()
    }

    private fun unregisterReceiver() {
        peerReceiver?.let {
            context.unregisterReceiver(it)
            peerReceiver = null
        }
    }

    //PUBLIC BELOW HERE
    public fun sendHostMessage(message: String) {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            Log.d("NetworkManager", "Sending message to all connected clients: $message")
            Log.d("NetworkManager", connectedClients.toString())
            for (client in connectedClients) {
            //    Thread {
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
         //   }.start()
        }
        } else {
            Log.e("NetworkManager", "Server socket is not open. Cannot send message.")
        }
    }

    public fun sendClientMessage(message: String) {

    }
    public fun initAsClient() {
        //discoverServices {  }
        //openClientSocket()
    }
    public fun initAsHost() {
        advertiseService()
        startServer()
    }
    public fun getMessage(): String? {
        return latestMessage
    }
*/
}
