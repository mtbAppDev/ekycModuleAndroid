package com.example.loanappandroid

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.ObjectsCompat.hash
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.mtb.mobilebanking.AndroidCypher
import com.mtb.mobilebanking.BroadcastCallBack
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import java.util.Collections
import java.util.UUID

class KycActivity : AppCompatActivity(), OtpReceivedInterface, BroadcastCallBack {
    var channel: MethodChannel? = null
    var channelName = "smartbanking.mtb.com/data";
    companion object {
        const val ENGINE_ID: String = "ekyc_engine_id"
        private const val FRAGMENT_TAG = "flutter_fragment"
    }

    private var cypher: AndroidCypher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc)

        // Setup window insets (optional for edge-to-edge UI)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initFlutterEngine()
        attachFlutterFragment()
    }

    private fun initFlutterEngine() {
        var flutterEngine = FlutterEngineCache.getInstance().get(ENGINE_ID)
        if (flutterEngine == null) {
            flutterEngine = FlutterEngine(this).apply {
                navigationChannel.setInitialRoute("/SignInAgent")
                dartExecutor.executeDartEntrypoint(
                    DartExecutor.DartEntrypoint.createDefault()
                )
            }
            FlutterEngineCache.getInstance().put(ENGINE_ID, flutterEngine)
        }

        setMethodChannels(flutterEngine)

    }

    private fun attachFlutterFragment() {
        // Avoid duplicates
        val existingFragment =
            supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? FlutterFragment

        if (existingFragment == null) {
            val flutterFragment = FlutterFragment
                .withCachedEngine(ENGINE_ID)
                .shouldAttachEngineToActivity(true)
                .build<FlutterFragment>()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, flutterFragment, FRAGMENT_TAG)
                .commit()
        }
    }

    private fun setMethodChannels(flutterEngine: FlutterEngine) {

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getEncrypted" -> {
                        val cypher = AndroidCypher()
                        val data = call.argument<String>("data")
                        try {
                            val res = cypher.encrypt(data ?: "")
                            result.success(res)
                        } catch (e: Exception) {
                            result.success(data)
                        }
                    }

                    "getDecrypted" -> {
                        val cypher = AndroidCypher()
                        val data = call.argument<String>("data")
                        try {
                            val res = cypher.decrypt(data ?: "")
                            result.success(res)
                        } catch (e: Exception) {
                            result.success(data)
                        }
                    }

                    "appSignature" -> {
                        val helper = AppSignatureHelper(this)
                        val otpAutoRefCode = helper.appSignatures[0]
                        startSMSListener()
                        SmsBroadcastReceiver.callBack = this;
//                registerReceiver(responseReceiver, null)
                        result.success(otpAutoRefCode)
                    }
                    "deviceActivationKey"->{
                        result.success("{\"encrToken\":\"cfFDD0zK+OBglUMVOjVUZuhplD5kFP4+WA2Xn/2NuBc=\"}")
                    }
                    "getSecureAndroidID"->{
                        var uniqueID: String =
                            PrefHelper.getInstance(this).prefGetIsDeviceUniqueId()
                        if (uniqueID.isEmpty()) {
                            uniqueID = UUID.randomUUID().toString()
                        }
                        PrefHelper.getInstance(this).prefSetIsDeviceUniqueId(uniqueID)
                        result.success(uniqueID)
                    }

                    else -> result.notImplemented()
                }
            }
    }


    override fun onOtpReceived(otp: String?) {
        TODO("Not yet implemented")
    }

    override fun onOtpTimeout() {
        TODO("Not yet implemented")
    }

    fun startSMSListener() {
        val mClient = SmsRetriever.getClient(this)
        val mTask = mClient.startSmsRetriever()
        mTask.addOnSuccessListener {
            //Toast.makeText(getContext(), "SMS Retriever starts", Toast.LENGTH_LONG).show();
        }
        mTask.addOnFailureListener {
            //Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
        }
    }

    override fun receivedValue(value: String) {
        channel!!.invokeMethod(
            "updateMessage",
            Collections.singletonMap<String, String>("message", value)
        )


}

    fun getAppSignatures(): ArrayList<String>? {
        val appCodes = ArrayList<String>()
        try {
            // Get all package signatures for the current package
            val packageName = packageName
            val packageManager = packageManager
            val signatures = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures

            // For each signature create a compatible hash
            if (signatures != null) {
                for (signature in signatures) {
                    val hash: String = hash(
                        packageName,
                        signature.toCharsString()
                    ).toString()
                    if (hash != null) {
                        appCodes.add(String.format("%s", hash))
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //Log.e(TAG, "Unable to find package to obtain hash.", e);
        }
        return appCodes
    }
}
