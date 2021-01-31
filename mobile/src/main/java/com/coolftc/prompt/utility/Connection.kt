package com.coolftc.prompt.utility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.lang.Exception

/*
    For documentation on this class see: https://github.com/M7mdZain/InternetConnectivityLibrary
    NOTE: Removed the use of lifecycle management to avoid limiting context to things like
     activities.  Instead made the class closeable to clear the listeners.  This allow one:
     val result = Connection(context).use { resource -> resource.isOnline();}
     Or just call close() manually when done (e.g. try/finally).
 */
class Connection(private var mContext: Context) : Closeable {

    companion object NetworkType {

        /**
         * Indicates this network uses a Cellular transport.
         */
        const val TRANSPORT_CELLULAR = 0

        /**
         * Indicates this network uses a Wi-Fi transport.
         */
        const val TRANSPORT_WIFI = 1

        /**
         * Indicates there is no available network.
         */
        private const val NO_NETWORK_AVAILABLE = -1

    }

    private var mConnectivityMgr: ConnectivityManager? = null

    private var mNetworkStateReceiver: NetworkStateReceiver? = null

    /*
     * boolean indicates if my device is connected to the internet or not
     * */
    private var mIsConnected = false
    private var mConnectionMonitor: ConnectionMonitor? = null



    interface ConnectionStateListener {
        fun onAvailable(isAvailable: Boolean)
    }

    init {
        mConnectivityMgr = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectionMonitor = ConnectionMonitor()
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            mConnectivityMgr!!.registerNetworkCallback(networkRequest, mConnectionMonitor!!)
        }
    }


    /**
     * Returns true if connected to the internet, and false otherwise
     *
     * NetworkInfo is deprecated in API 29
     * https://developer.android.com/reference/android/net/NetworkInfo
     *
     * getActiveNetworkInfo() is deprecated in API 29
     * https://developer.android.com/reference/android/net/ConnectivityManager#getActiveNetworkInfo()
     *
     * getNetworkInfo(int) is deprecated as of API 23
     * https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkInfo(int)
     */
    fun isOnline(): Boolean {
        mIsConnected = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Checking internet connectivity
            var activeNetwork: NetworkInfo? = null
            if (mConnectivityMgr != null) {
                activeNetwork = mConnectivityMgr!!.activeNetworkInfo // Deprecated in API 29
            }
            mIsConnected = activeNetwork != null
        } else {
            val allNetworks = mConnectivityMgr!!.allNetworks // added in API 21 (Lollipop)
            for (network in allNetworks) {
                val networkCapabilities = mConnectivityMgr!!.getNetworkCapabilities(network)
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    )
                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        ) mIsConnected = true
                }
            }
        }
        return mIsConnected
    }


    /**
     * Returns:
     *
     * NO_NETWORK_AVAILABLE >>> when you're offline
     * TRANSPORT_CELLULAR >> When Cellular is the active network
     * TRANSPORT_WIFI >> When Wi-Fi is the Active network
     */
    fun getActiveNetwork(): Int {
        val activeNetwork = mConnectivityMgr!!.activeNetworkInfo // Deprecated in API 29
        if (activeNetwork != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = mConnectivityMgr!!.getNetworkCapabilities(
                mConnectivityMgr!!.activeNetwork
            )
            if (capabilities != null) if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // connected to mobile data
                return TRANSPORT_CELLULAR
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // connected to wifi
                return TRANSPORT_WIFI
            }
        } else {
            if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) { // Deprecated in API 28
                // connected to mobile data
                return TRANSPORT_CELLULAR
            } else if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) { // Deprecated in API 28
                // connected to wifi
                return TRANSPORT_WIFI
            }
        }
        return NO_NETWORK_AVAILABLE
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getAvailableNetworksCount(): Int {
        var count = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val allNetworks = mConnectivityMgr!!.allNetworks // added in API 21 (Lollipop)
            for (network in allNetworks) {
                val networkCapabilities = mConnectivityMgr!!.getNetworkCapabilities(network)
                if (networkCapabilities != null) if (networkCapabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                ) count++
            }
        }
        return count
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getAvailableNetworks(): List<Int> {
        val activeNetworks: MutableList<Int> = ArrayList()
        val allNetworks: Array<Network> // added in API 21 (Lollipop)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            allNetworks = mConnectivityMgr!!.allNetworks
            for (network in allNetworks) {
                val networkCapabilities = mConnectivityMgr!!.getNetworkCapabilities(network)
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) activeNetworks.add(TRANSPORT_WIFI)
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) activeNetworks.add(TRANSPORT_CELLULAR)
                }
            }
        }
        return activeNetworks
    }

    /*
        This method allow caller to listen for network changes.
     */
    fun onInternetStateListener(listener: ConnectionStateListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mNetworkStateReceiver = NetworkStateReceiver(listener)
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            mContext.registerReceiver(mNetworkStateReceiver, intentFilter)
        } else {
            mConnectionMonitor!!.setOnConnectionStateListener(listener)
        }
    }

    /*
        Allows resource management for the listeners.
     */
    override fun close() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mConnectionMonitor != null)
                    mConnectivityMgr!!.unregisterNetworkCallback(mConnectionMonitor!!)
            } else {
                if (mNetworkStateReceiver != null) mContext.unregisterReceiver(mNetworkStateReceiver)
            }
        } catch (ex: Exception) {
            ExpClass.logEX(ex, javaClass.name)
        }
    }

    inner class NetworkStateReceiver(var mListener: ConnectionStateListener) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras != null) {
                val activeNetworkInfo: NetworkInfo? =
                    mConnectivityMgr?.getActiveNetworkInfo() // deprecated in API 29

                /*
                 * activeNetworkInfo.getState() deprecated in API 28
                 * NetworkInfo.State.CONNECTED deprecated in API 29
                 */
                if (!mIsConnected && activeNetworkInfo != null && activeNetworkInfo.state == NetworkInfo.State.CONNECTED) {
                    mIsConnected = true
                    mListener.onAvailable(true)
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, java.lang.Boolean.FALSE)) {
                    if (!isOnline()) {
                        mListener.onAvailable(false)
                        mIsConnected = false
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    inner class ConnectionMonitor : ConnectivityManager.NetworkCallback() {
        private var mConnectionStateListener: ConnectionStateListener? = null
        fun setOnConnectionStateListener(connectionStateListener: ConnectionStateListener?) {
            mConnectionStateListener = connectionStateListener
        }

        override fun onAvailable(network: Network) {
            if (mIsConnected) return
            if (mConnectionStateListener != null) {
                mConnectionStateListener!!.onAvailable(true)
                mIsConnected = true
            }
        }

        override fun onLost(network: Network) {
            if (getAvailableNetworksCount() == 0) {
                mConnectionStateListener?.onAvailable(false)
                mIsConnected = false
            }
        }
    }

}