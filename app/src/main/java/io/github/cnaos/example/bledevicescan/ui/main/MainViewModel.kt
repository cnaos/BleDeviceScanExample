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
        // スキャン時間
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)

        private val TAG = "MainViewModel"

        // BLEデバイスのソートに使う
        // デバイス名(nullは最後)、デバイスのMACアドレスの順
        private val BLE_DEVICE_COMPARATOR =
            compareBy<BluetoothDevice, String?>(nullsLast()) { it.name }
                .thenBy { it.address }
    }

    // Activityから渡されたBluetoothAdapter
    lateinit var bluetoothAdapter: BluetoothAdapter

    // BLE関係のRuntimePermissionの処理用(Peko)
    val permissionLiveData = PermissionsLiveData()

    // ActivityでBLE関係のRuntimePermissionがあるかどうか設定される
    var isGrantedBLEPermission = false

    // BLEデバイスのスキャンを行うJob
    private var deviceScanJob: Job? = null

    // スキャンしたBLEデバイスを記録しておくMap
    private val scannedDeviceMap = ConcurrentHashMap<String, BluetoothDevice>()

    // BLEデバイスのスキャン状態の画面表示用
    val scanning = MutableLiveData<Boolean>(false)

    // 画面表示用のデバイスリスト
    val bleDeviceDataList = MutableLiveData<List<BluetoothDevice>>(listOf<BluetoothDevice>())


    /**
     * ログ出力用のメソッド
     */
    fun log(functionName: String, msg: String = "") =
        Log.i(TAG, "[${Thread.currentThread().name}] $functionName $msg")


    /**
     * BLEデバイスのスキャン結果をFlowで取得する
     */
    fun deviceScanFlow(
        scanner: BluetoothLeScanner
    ): Flow<ScanResult> = callbackFlow {
        val functionName = "deviceScanFlow()"
        val mLeScanCallback = object : ScanCallback() {
            // BLEデバイスがスキャンされると呼ばれる。
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (channel.isClosedForSend) {
                    return
                }
                // callbackFlowのchannelにScanResultを送る
                offer(result)
            }
        }

        // BLEデバイスのスキャン処理開始
        scanner.startScan(mLeScanCallback)

        // 一定時間経過したらchannelをcloseするタイマーを仕掛ける
        launch {
            delay(SCAN_PERIOD)
            log(functionName, "channel close delay")
            channel.close()
        }

        // callbackFlowのchannelが閉じるか、利用側でjobがキャンセルされた場合の処理
        awaitClose {
            log(functionName, "channel closed")
            scanner.stopScan(mLeScanCallback)
            stopDeviceScan()
        }
    }

    /**
     * BLEデバイスのスキャンを開始する
     */
    fun startDeviceScan() {
        log("startDeviceScan")

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

    /**
     * BLEデバイスのスキャンを停止する
     */
    fun stopDeviceScan() {
        log("stopDeviceScan")
        scanning.postValue(false)
        cancelDeviceScanJob()
    }

    /**
     * BLEデバイスのスキャンJobをキャンセルする
     */
    fun cancelDeviceScanJob() {
        log("cancel deviceScanJob")
        deviceScanJob?.cancel()
    }

    /**
     * スキャンしたBLEデバイスを記録して、画面表示用のデバイスリストに反映する
     */
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
