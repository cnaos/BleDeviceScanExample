package io.github.cnaos.example.bledevicescan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.markodevcic.peko.PermissionResult
import io.github.cnaos.example.bledevicescan.ui.main.MainFragment
import io.github.cnaos.example.bledevicescan.ui.main.MainViewModel
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {
    // 定数
    companion object {
        private const val REQUEST_ENABLE_BLUETOOTH = 1 // Bluetooth機能の有効化要求時の識別コード
        private const val TAG = "MainActivity"
    }

    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            alert("Bluetooth Adapterが見つからないため終了します。") {
                okButton { finish() }
            }.show()
        } else {
            viewModel.bluetoothAdapter = bluetoothAdapter
        }

        // パーミッションリクエスト後の処理を設定する
        setupProcessPermissionResult()

        // パーミッションがあるか確認
        viewModel.isGrantedBLEPermission = isGrantedBlePermission()

        // 端末のBluetooth機能が有効になっているか確認
        requestEnableBluetoothFeature()
    }

    override fun onPause() {
        super.onPause()

        // スキャンの停止
        viewModel.stopDeviceScan()
    }


    private fun setupProcessPermissionResult() {
        viewModel.permissionLiveData.observe(this, Observer { result: PermissionResult ->
            Log.i(TAG, "process PermissionResult: $result")

            when (result) {
                is PermissionResult.Granted -> {
                    viewModel.isGrantedBLEPermission = isGrantedBlePermission()
                    toast("permission granted")
                }
                is PermissionResult.Denied.JustDenied -> {
                    // at least one permission was denied
                    alert("this application need permission for BLE") {
                        okButton { }
                    }.show()
                }
                is PermissionResult.Denied.NeedsRationale -> {
                    // user clicked Deny, let's show a rationale
                    alert("アプリを利用するには 位置情報 の権限が必要です") {
                        okButton { }
                    }.show()
                }
                is PermissionResult.Denied.DeniedPermanently -> {
                    // Android System won't show Permission dialog anymore, let's tell the user we can't proceed
                    alert("アプリを利用するには、アプリの権限で 位置情報 を許可してください。") {
                        okButton { finish() }
                    }.show()
                }
            }
        })
    }

    private fun isGrantedBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestEnableBluetoothFeature() {
        if (viewModel.bluetoothAdapter.isEnabled) {
            return
        }
        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(
            enableBtIntent,
            REQUEST_ENABLE_BLUETOOTH
        )
    }

}
