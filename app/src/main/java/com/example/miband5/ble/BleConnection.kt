package com.example.miband5.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
class BleConnection(
    private val context: Context,
    private val authKey: ByteArray?,
    private val onState: (BleConnectionState) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null
    private var authCharacteristic: BluetoothGattCharacteristic? = null
    private var scanning = false
    private var shouldAutoReconnect = true

    var notifyListener: ((BluetoothGattCharacteristic, ByteArray) -> Unit)? = null
    var writeListener: ((BluetoothGattCharacteristic, Int) -> Unit)? = null
    var onAuthenticated: (() -> Unit)? = null

    private val authFlags = byteArrayOf(MiBand5Auth.FLAG_BAND5, MiBand5Auth.FLAG_LEGACY)
    private var authFlagIndex = 0

    private var pendingReadCont: CancellableContinuation<ByteArray?>? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: return
            val hasHuamiService = result.scanRecord?.serviceUuids?.any {
                val u = it.uuid
                u == MiBand5Gatt.UUID_SERVICE_MIBAND ||
                    u == MiBand5Gatt.UUID_SERVICE_MIBAND2 ||
                    u == MiBand5Gatt.UUID_SERVICE_MIBAND_HUAMI ||
                    u == MiBand5Gatt.UUID_SERVICE_MIBAND2_HUAMI
            } == true
            val looksLikeBand =
                name.contains("Mi", ignoreCase = true) ||
                    name.contains("Amazfit", ignoreCase = true) ||
                    name.contains("Band", ignoreCase = true)
            if (hasHuamiService || looksLikeBand) {
                stopScan()
                onState(BleConnectionState.Found(name, device.address))
                connect(device)
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onState(BleConnectionState.Error("Bluetooth is off"))
            return
        }
        if (scanning) return
        scanning = true
        onState(BleConnectionState.Scanning)
        bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
            ?: onState(BleConnectionState.Error("BLE scanner unavailable"))
        mainHandler.postDelayed({
            if (scanning) {
                stopScan()
                onState(BleConnectionState.Error("No band found. Keep it close and try again."))
            }
        }, 20_000)
    }

    fun stopScan() {
        if (!scanning) return
        scanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun connectTo(address: String) {
        stopScan()
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (device == null) {
            onState(BleConnectionState.Error("Unknown device $address"))
            return
        }
        onState(BleConnectionState.Connecting)
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun connect(device: BluetoothDevice) {
        stopScan()
        onState(BleConnectionState.Connecting)
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        val g = gatt ?: return null
        for (service in g.services) {
            service.getCharacteristic(uuid)?.let { return it }
        }
        return null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    onState(BleConnectionState.DiscoveringServices)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    this@BleConnection.gatt?.close()
                    this@BleConnection.gatt = null
                    onState(BleConnectionState.Disconnected("connection lost"))
                    if (shouldAutoReconnect) {
                        mainHandler.postDelayed({ startScan() }, 3000)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onState(BleConnectionState.Error("service discovery failed"))
                gatt.disconnect()
                return
            }
            val auth = findCharacteristic(MiBand5Gatt.UUID_CHARACTERISTIC_AUTH)
            if (auth == null) {
                onState(BleConnectionState.Error("auth characteristic not found"))
                gatt.disconnect()
                return
            }
            authCharacteristic = auth

            val notifyEnabled = gatt.setCharacteristicNotification(auth, true)
            val cccd = auth.getDescriptor(MiBand5Gatt.UUID_DESCRIPTOR_CCCD)
            if (!notifyEnabled || cccd == null) {
                onState(BleConnectionState.Error("could not enable auth notifications"))
                gatt.disconnect()
                return
            }
            onState(BleConnectionState.Authenticating)
            cccd.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: android.bluetooth.BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == MiBand5Gatt.UUID_DESCRIPTOR_CCCD &&
                descriptor.characteristic?.uuid == MiBand5Gatt.UUID_CHARACTERISTIC_AUTH
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    startHandshake()
                } else {
                    onState(BleConnectionState.Error("could not enable auth notifications"))
                    gatt.disconnect()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            writeListener?.invoke(characteristic, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                pendingReadCont?.resume(characteristic.value)
            } else {
                pendingReadCont?.resume(null)
            }
            pendingReadCont = null
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                MiBand5Gatt.UUID_CHARACTERISTIC_AUTH -> handleAuthNotification(characteristic.value)
                else -> notifyListener?.invoke(characteristic, characteristic.value)
            }
        }
    }

    private fun currentFlag(): Byte = authFlags[authFlagIndex]

    private fun startHandshake() {
        val key = authKey
        if (key == null) {
            onState(
                BleConnectionState.Error(
                    "Auth key required. Paste the 32-hex-char key from Zepp / Gadgetbridge / huami-token."
                )
            )
            return
        }
        writeAuth(MiBand5Auth.sendKeyFrame(key, currentFlag()))
    }

    private fun writeAuth(bytes: ByteArray) {
        val ch = authCharacteristic ?: return
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt?.writeCharacteristic(ch)
    }

    private fun handleAuthNotification(data: ByteArray) {
        when (val resp = MiBand5Auth.parseAuthResponse(data)) {
            is MiBand5Auth.AuthResponse.KeyRejected -> {
                onState(BleConnectionState.Error("Auth key rejected by band"))
            }
            is MiBand5Auth.AuthResponse.KeyAccepted -> {
                writeAuth(MiBand5Auth.requestRandomFrame(currentFlag()))
            }
            is MiBand5Auth.AuthResponse.RandomReceived -> {
                val key = authKey ?: return
                writeAuth(MiBand5Auth.sendEncryptedRandomFrame(key, resp.random, currentFlag()))
            }
            is MiBand5Auth.AuthResponse.Authenticated -> {
                authFlagIndex = 0
                onState(BleConnectionState.Connected)
                onAuthenticated?.invoke()
            }
            is MiBand5Auth.AuthResponse.AuthFailed -> {
                if (authFlagIndex < authFlags.lastIndex) {
                    authFlagIndex++
                    startHandshake()
                } else {
                    onState(BleConnectionState.Error("Authentication failed. Check the key."))
                }
            }
            else -> { }
        }
    }

    fun readCharacteristic(charUuid: UUID): Boolean {
        val ch = findCharacteristic(charUuid) ?: return false
        return gatt?.readCharacteristic(ch) ?: false
    }

    fun readCharacteristic(serviceUuid: UUID, charUuid: UUID): Boolean {
        val ch = gatt?.getService(serviceUuid)?.getCharacteristic(charUuid)
            ?: findCharacteristic(charUuid)
            ?: return false
        return gatt?.readCharacteristic(ch) ?: false
    }

    fun writeCharacteristic(
        serviceUuid: UUID,
        charUuid: UUID,
        bytes: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): Boolean {
        val ch = gatt?.getService(serviceUuid)?.getCharacteristic(charUuid)
            ?: findCharacteristic(charUuid)
            ?: return false
        ch.value = bytes
        ch.writeType = writeType
        return gatt?.writeCharacteristic(ch) ?: false
    }

    fun enableNotifications(serviceUuid: UUID, charUuid: UUID): Boolean {
        val ch = gatt?.getService(serviceUuid)?.getCharacteristic(charUuid)
            ?: findCharacteristic(charUuid)
            ?: return false
        if (gatt?.setCharacteristicNotification(ch, true) != true) return false
        val cccd = ch.getDescriptor(MiBand5Gatt.UUID_DESCRIPTOR_CCCD) ?: return false
        cccd.value = byteArrayOf(0x01, 0x00)
        return gatt?.writeDescriptor(cccd) ?: false
    }

    suspend fun readCharacteristicSuspend(charUuid: UUID): ByteArray? =
        suspendCancellableCoroutine { cont ->
            val ok = readCharacteristic(charUuid)
            if (!ok) {
                cont.resume(null)
            } else {
                pendingReadCont = cont
            }
        }

    suspend fun readCharacteristicSuspend(serviceUuid: UUID, charUuid: UUID): ByteArray? =
        suspendCancellableCoroutine { cont ->
            val ok = readCharacteristic(serviceUuid, charUuid)
            if (!ok) {
                cont.resume(null)
            } else {
                pendingReadCont = cont
            }
        }

    fun disconnect() {
        shouldAutoReconnect = false
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        onState(BleConnectionState.Idle)
    }
}
