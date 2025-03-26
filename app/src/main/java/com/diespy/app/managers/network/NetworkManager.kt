import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.diespy.app.managers.firestore.FireStoreManager
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

ALWAYS USE  sendHostMessage(), sendClientMessage() and getMessage()!


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
    var latestMessage: String? = null
    private val fireStoreManager = FireStoreManager()

    //advertise stays
    private fun advertiseService(groupName: String, groupId: String, groupUserCount: String) {
        val record = hashMapOf(
            "service_name" to "DiespyApp",
            "groupName" to groupName,
            "groupID" to groupId,
            "groupUserCount" to groupUserCount
        )
        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("DiespyService", "_diespy._tcp", record)

        //Permission checking code autofilled by android studio
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {Log.e("NetworkManager", "Permissions not granted for Service Advertising")}
        wifiP2pManager?.addLocalService(
            channel,
            serviceInfo,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("NetworkManager", "Service advertised successfully.")
                }
                override fun onFailure(code: Int) {
                    Log.e("NetworkManager", "Failed to advertise service: $code")
                }
            })
    }

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

        //TextListener to actually read the info
        val txtRecordListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomainName, txtRecordMap, srcDevice ->
            Log.d("NetworkManager", "TXT Record received: $txtRecordMap")
            val groupName = txtRecordMap["groupName"] as String ?: "Unnamed Group"
            val groupId = txtRecordMap["groupId"] as String ?: "N/A"
            val groupUserCount =  txtRecordMap["groupUserCount"] as String ?: "N/A"
            srcDevice.deviceAddress?.let { deviceAddress ->
                discoveredDeviceMap[deviceAddress] = PartyItem(groupId, groupName, groupUserCount.toInt())
                //discoveredDeviceMap[deviceAddress] = Triple(hostIp, hostPort.toInt(), groupName)
            }
        }

        wifiP2pManager?.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener)
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
                // Starts a handler to search for peers in the background, after 10 loops it forces a callback with the empty list
                Handler(Looper.getMainLooper()).postDelayed({
                    // Asynchronously requests peers. Internal logic (peerlist ->) runs only upon completion.
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.e("NetworkManager", "Permissions not granted for service discovery.")
                        return@postDelayed
                    }

                    //PEER DISCOVERY:
                    wifiP2pManager?.requestPeers(channel) { peerList ->
                        if (peerList.deviceList.isNotEmpty()) {
                            Log.d("NetworkManager", "Peers discovered: ${peerList.deviceList.size}")
                            // Add service request before starting service discovery
                            val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
                            wifiP2pManager?.addServiceRequest(channel, serviceRequest, object : WifiP2pManager.ActionListener {
                                @SuppressLint("MissingPermission")
                                override fun onSuccess() {
                                    Log.d("NetworkManager", "Service request added successfully.")
                                    // Start service discovery
                                    wifiP2pManager?.discoverServices(channel, object : WifiP2pManager.ActionListener {
                                        override fun onSuccess() {
                                            Log.d("NetworkManager", "Service discovery started successfully.")
                                        }
                                        override fun onFailure(code: Int) {
                                            Log.e("NetworkManager", "Service discovery failed: $code")
                                        }
                                    })
                                }

                                override fun onFailure(code: Int) {
                                    Log.e("NetworkManager", "Failed to add service request: $code")
                                }
                            })
                        } else {
                            // Forces return of empty list after 10 seconds of handler not found.
                            Log.e("NetworkManager", "No peers found.")
                            discoveryCallback?.invoke(emptyList())
                        }
                    }
                }, 10000) // 10 second search period to find devices and services.
            }

            override fun onFailure(code: Int) {
                Log.e("NetworkManager", "Peer discovery failed: $code")
            }
        })

    }

    public fun initAsHost() {
        val partyName : String = SharedPrefManager.getCurrentPartyName(context) ?: "Unnamed Party"
        val partyId :String = SharedPrefManager.getCurrentPartyId(context) ?: "N/A"
        val partyUC :String = SharedPrefManager.getCurrentPartyUserCount(context) ?: "N/A"
        advertiseService(partyName, partyId, partyUC)

    }
    public fun getMessage(): String {
        if(latestMessage.isNullOrEmpty())
            return ""
        else
            return latestMessage!!
    }

}
