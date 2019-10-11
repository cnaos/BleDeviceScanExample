# BLEデバイスのスキャンをkotlin coroutineのcallbackFlowでやる

## 通常のBLEデバイスのスキャン方法


BLEデバイスのスキャン処理部分だけ取り出すとこんな感じ。

```kotlin
// スキャン時間
private val SCAN_PERIOD: Long =  
    TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)
    
// スキャンのタイムアウト処理用
private val handler: Handler = Handler()

// デバイススキャンコールバック  
private val mLeScanCallback = object : ScanCallback() {  
    override fun onScanResult(callbackType: Int, result: ScanResult) {  
        // スキャンしたBLEデバイスの処理  
        addDevice(result)  
    }  
}

fun startDeviceScan() {	
	// タイムアウト処理の仕込み
    handler.postDelayed({
        scanner.stopScan(mLeScanCallback)
    }, SCAN_PERIOD)

    // BLEデバイスのスキャン開始
    scanner.startScan(mLeScanCallback)
}
```

### スキャンのコールバック  

```kotlin
// デバイススキャンコールバック  
private val mLeScanCallback = object : ScanCallback() {  
    override fun onScanResult(callbackType: Int, result: ScanResult) {  
        // スキャンしたBLEデバイスの処理  
        addDevice(result)  
    }  
}
```

BLEデバイスを検出した際にBluetoothLeScannerから呼ばれるcallback用のオブジェクト(mLeScanCallback)を用意します。

### スキャン処理

```kotlin
fun startDeviceScan() {	
    /// 省略 ///

    // BLEデバイスのスキャン開始
    scanner.startScan(mLeScanCallback)
}
```

BluetoothLeScannerのstartScanメソッドの引数にmLeScanCallbackを渡してstartScanメソッドを呼び出すとBLEデバイスのスキャンが実行されます。

BluetoothLeScannerがBLEデバイスを検出されると、mLeScanCallbackのonScanResultメソッドが呼ばれるので、
onScanResultメソッドで検出したBLEデバイスを処理します。
例ではaddDeviceメソッドで処理しています。

### タイムアウト処理

```kotlin
fun startDeviceScan() {	
    // タイムアウト処理の仕込み
    handler.postDelayed({
        scanner.stopScan(mLeScanCallback)
    }, SCAN_PERIOD)

    // BLEデバイスのスキャン開始
}
```



また、一定時間経過したあとにBLEデバイスのスキャンを停止するために
HandlerをつかってBluetoothLeScannerのstopScanメソッドを呼び出します。


## callbackFlowを使った方法

[callbackFlowのリファレンス](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html)

### Flowを提供する側の実装

```kotlin

// 定数
companion object {
    // スキャン時間
    private val SCAN_PERIOD: Long =
        TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)
}

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

```


### Flowを利用する側の実装

```kotlin

// BLEデバイスのスキャンを行うJob
private var deviceScanJob: Job? = null

fun startDeviceScan() {
    log("startDeviceScan")

    // RuntimePermissionとかの処理

    // BLEデバイスのcallbackFlowを作ってそこからデータを受け取る
    deviceScanJob = viewModelScope.launch(Dispatchers.IO) {
        val scanFlow = deviceScanFlow(scanner)
        scanFlow.buffer().collect {
            addDevice(it.device)
        }
    }
}

/**
 * BLEデバイスのスキャンJobをキャンセルする
 */
fun cancelDeviceScanJob() {
    log("cancel deviceScanJob")
    deviceScanJob?.cancel()
}

 ```

### 解説

私のイメージだと、コールバック処理をcallbackFlowで作ったFlow内に閉じ込めて、使う側ではFlowで流れてきたコールバックの結果を処理するという感じです。


#### スキャン処理

```kotlin
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
```

通常の方法だと、onScanResult内で直接スキャン結果を処理していましたが、callbackFlowを使った方法では、検出したBLEデバイスの情報(ScanResult)をofferメソッドを使ってかcallbackFlowが持っているchannelに送ります。

#### スキャン結果の受信処理

```Kotlin
    // BLEデバイスのcallbackFlowを作ってそこからデータを受け取る
    deviceScanJob = viewModelScope.launch(Dispatchers.IO) {
        val scanFlow = deviceScanFlow(scanner)
        scanFlow.buffer().collect {
            addDevice(it.device)
        }
    }
```

送ったスキャン結果はFlowを使う側でcollectで受信して処理します。


#### タイムアウト処理

```kotlin
    // 一定時間経過したらchannelをcloseするタイマーを仕掛ける
    launch {
        delay(SCAN_PERIOD)
        log(functionName, "channel close delay")
        channel.close()
    }
```

通常のBLEデバイスのスキャンの例ではHandlerを使って実装していたスキャンのタイムアウト処理をcoroutineのdelayを使って実装しています。
タイマーでBluetoothLEScannerのstopScanを呼び出す代わりに、タイマーでcallbackFlowのchannelをcloseします。


#### コールバックの解除処理

```kotlin

    // callbackFlowのchannelが閉じるか、利用側でjobがキャンセルされた場合の処理
    awaitClose {
        log(functionName, "channel closed")
        scanner.stopScan(mLeScanCallback)
        stopDeviceScan()
    }
```

最後のawaitCloseのブロックは、
callbackFlowのchannelがcloseされた場合、または、callbackFlowを含むJobがキャンセルされた場合に、ブロック内の処理が実行されます。


## 参考にさせていただいたもの

[GoogleのBLEデバイスのスキャン処理の説明](https://developer.android.com/guide/topics/connectivity/bluetooth-le?hl=ja#find)

[kotlinのcallbackFlowの公式ドキュメント](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html)

[BLEデバイスを検出する処理の作成](https://www.hiramine.com/programming/blecommunicator/02_scan_device.html)

[Callbackから定期的に受け取る位置情報をKotlin Coroutines Flowを使って監視する方法](https://qiita.com/akihito-okada/items/f4b6e937fbd954756a65)
