package com.diespy.app.managers.network
import android.content.Context

/*
Because we need only one instance networkmanager, any accesses to it will happen through the
publicnetworkmanager class. The actual source code is found in networkmanager.kt
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