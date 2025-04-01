package com.diespy.app.managers.network
import android.content.Context

/*Because we need only one networkmanager, any accesses to it will happen through the publicnetworkmanager class

The actual source code is found in networkmanager.kt

To access, reference NetworkManagerObject.getInstance(context) can use requireContext() for context
 */
object PublicNetworkManager {
@Volatile
private var instance: NetworkManager? = null

fun getInstance(context: Context): NetworkManager {

    return instance ?: synchronized(this) {
        instance ?: NetworkManager(context.applicationContext).also { instance = it }
    }
}
}