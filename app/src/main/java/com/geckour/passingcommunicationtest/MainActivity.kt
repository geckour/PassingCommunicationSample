package com.geckour.passingcommunicationtest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    enum class PermissionRequestCode {
        ACCESS_COARSE_LOCATION
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution?) {
            Timber.d("Connection result with device[$endpointId]: ${resolution?.status?.isSuccess}")

            if (resolution?.status?.isSuccess == true) {
                    Nearby.getConnectionsClient(this@MainActivity)
                            .sendPayload(endpointId, payload)
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.d("Disconnected from device[$endpointId]")
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo?) {
            Timber.d("Started connection with device[$endpointId]: ${connectionInfo?.endpointName}")

            Nearby.getConnectionsClient(this@MainActivity)
                    .acceptConnection(endpointId, object : PayloadCallback() {
                        override fun onPayloadReceived(endpointId: String, payload: Payload) {
                            val testPayload: TestPayload? = payload.asBytes()?.parse()
                            Timber.d("Received payload from[$endpointId]: $testPayload")
                            testPayload?.userId?.apply { textView.text = this }
                        }

                        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {

                        }
                    })
        }
    }

    private val endpointDiscoveryCallback = object: EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, endpointInfo: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(this@MainActivity)
                    .requestConnection("hoge", endpointId, connectionLifecycleCallback)
                    .addOnCompleteListener {
                        Toast.makeText(this@MainActivity, "Connection is started✨", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Timber.e(it)
                        Toast.makeText(this@MainActivity, "Connection is failed😨", Toast.LENGTH_SHORT).show()
                    }
        }

        override fun onEndpointLost(endpointId: String) {
            Toast.makeText(this@MainActivity, "Endpoint is lost🍃", Toast.LENGTH_SHORT).show()
        }

    }

    private val payload: Payload = Payload.fromBytes(TestPayload("nyan").toByteArray())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {}

        requestPermissions()
        Timber.d("Timber is WORKING✨")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionRequestCode.ACCESS_COARSE_LOCATION.ordinal -> {
                requestPermissions()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PermissionRequestCode.ACCESS_COARSE_LOCATION.ordinal)
        } else {
            startDiscovery()
            startAdvertising()
        }
    }

    private fun startAdvertising() {
        Nearby.getConnectionsClient(this).startAdvertising(
                "hoge",
                packageName,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnCompleteListener {
            Toast.makeText(this, "Advertising is started✨", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Timber.e(it)
            Toast.makeText(this, "Advertising is failed😨", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDiscovery() {
        Nearby.getConnectionsClient(this).startDiscovery(
                packageName,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnCompleteListener {
            Toast.makeText(this, "Discovering is started✨", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Timber.e(it)
            Toast.makeText(this, "Discovering is failed😨", Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T> T.toByteArray(): ByteArray = Gson().toJson(this).toByteArray(Charsets.UTF_8)

    private inline fun <reified T> ByteArray.parse(): T? =
            try {
                Gson().fromJson(this.toString(Charsets.UTF_8), T::class.java)
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
}
