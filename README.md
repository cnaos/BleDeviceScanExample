# BleDeviceScanExample

## これは何？

BLEデバイスのスキャンをkotlin coroutineの[callbackFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html)を使って行うサンプルアプリです。

<img src="https://github.com/cnaos/picture/raw/master/BleDeviceScanExample/screenshot01.png" width="25%"/>

googleの[Android BluetoothLeGatt Sample](https://github.com/android/connectivity-samples/tree/master/BluetoothLeGatt)
と、
HIRAMINEさんの[02．BLEデバイスを検出する処理の作成](https://www.hiramine.com/programming/blecommunicator/02_scan_device.html)
をベースにしています。

## 利用ライブラリ

* Peko
  * https://github.com/deva666/Peko
  * Android PermissionsをKotlin Coroutineまたは、LiveDataで扱えるようにするためのライブラリ
* android-identicons
  * https://github.com/lelloman/android-identicons
  * Ideticonを生成するためのライブラリ
