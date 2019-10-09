package io.github.cnaos.example.bledevicescan.ui.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.PermissionsLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間

        private val TAG = "MainViewModel"

        private val BLE_DEVICE_COMPARATOR =
            compareBy<BluetoothDevice, String?>(nullsLast()) { it.name }
                .thenBy { it.address }
    }

    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()

    var isGrantedBLEPermission = false

    val scanning = MutableLiveData<Boolean>(false)

    private var deviceScanJob: Job? = null

    private val scannedDeviceMap = ConcurrentHashMap<String, BluetoothDevice>()

    /**
     * 表示用のデバイスリスト
     */
    val bleDeviceDataList = MutableLiveData<List<BluetoothDevice>>(listOf<BluetoothDevice>())


    fun log(functionName: String, msg: String) =
        Log.i(TAG, "[${Thread.currentThread().name}] $functionName $msg")


    fun deviceScanFlow(
        scanner: BluetoothLeScanner
    ): Flow<ScanResult> = callbackFlow {
        val functionName = "deviceScanFlow()"
        val mLeScanCallback = object : ScanCallback() {
            // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (channel.isClosedForSend) {
                    return
                }
                offer(result)
            }
        }
        scanner.startScan(mLeScanCallback)

        // 一定時間経過したらchannelをcloseするタイマーを仕掛ける
        launch {
            delay(SCAN_PERIOD)
            log(functionName, "channel close delay")
            channel.close()
        }

        // Suspend until either onCompleted or external cancellation are invoked
        awaitClose {
            log(functionName, "channel closed")
            scanner.stopScan(mLeScanCallback)
            stopDeviceScan()
        }
    }

    fun startDeviceScan() {
        Log.i(TAG, "startDeviceScan")

        if (!isGrantedBLEPermission) {
            permissionLiveData.checkPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }

        // BluetoothLeScannerの取得
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "No BluetoothLeScanner available. Is Bluetooth turned on?")
            return
        }

        scanning.postValue(true)
        if (deviceScanJob != null && deviceScanJob!!.isActive) {
            deviceScanJob?.cancel()
        }

        // BLEデバイスのcallbackFlowを作ってそこからデータを受け取る
        deviceScanJob = viewModelScope.launch(Dispatchers.IO) {
            val scanFlow = deviceScanFlow(scanner)
            scanFlow.buffer().collect {
                addDevice(it.device)
            }
        }
    }

    fun stopDeviceScan() {
        Log.i(TAG, "stopDeviceScan")
        scanning.postValue(false)
        cancelDeviceScanJob()
    }

    fun cancelDeviceScanJob() {
        Log.i(TAG, "cancel deviceScanJob")
        deviceScanJob?.cancel()
    }


    fun addDevice(device: BluetoothDevice?) {
        val functionName = "addDevice()"
        device ?: return

        if (scannedDeviceMap.putIfAbsent(device.address, device) != null) {
            return
        }
        log(functionName, "scaned device=$device")

        val tmpList = scannedDeviceMap.values.sortedWith(BLE_DEVICE_COMPARATOR)
        bleDeviceDataList.postValue(tmpList)
    }


}
