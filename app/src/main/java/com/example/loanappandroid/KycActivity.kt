package com.example.loanappandroid

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.mtb.mobilebanking.AndroidCypher
import com.mtb.mobilebanking.BroadcastCallBack
import com.yalantis.ucrop.UCrop
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.util.UUID


class KycActivity : AppCompatActivity(), BroadcastCallBack {

    companion object {
        const val ENGINE_ID: String = "ekyc_engine_id"
        private const val FRAGMENT_TAG = "flutter_fragment"
        private const val CHANNEL_NAME = "smartbanking.mtb.com/data"
        private const val REQUEST_CAMERA_PERMISSION = 200


    }

    private var channel: MethodChannel? = null
    private var pendingResult: Result? = null
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var capturedImageUri: Uri
    private var flutterFragment: FlutterFragment? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )


        // Handle insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        initFlutterEngine()
        attachFlutterFragment()


        // Register captureImageLauncher
        captureImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { fileResult ->
            startCropActivity(capturedImageUri)
        }

        cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { regResult ->
            if (regResult.resultCode == FragmentActivity.RESULT_OK) {
                val data = regResult.data
                if (data != null) {
                    val croppedUri = UCrop.getOutput(data)
                    if (croppedUri != null) {
                        val path = croppedUri.path ?: ""
                        pendingResult?.success("""{"error":"","image":"$path"}""")
                        pendingResult = null // clear after using
                    } else {
                        pendingResult?.success("""{"error":"Crop failed","image":""}""")
                        pendingResult = null
                    }
                } else {
                    pendingResult?.success("""{"error":"No crop data","image":""}""")
                    pendingResult = null
                }
            } else {
                pendingResult?.success("""{"error":"Crop cancelled","image":""}""")
                pendingResult = null
            }
        }





// Register pickImageLauncher
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == FragmentActivity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { pickedImageUri ->
                    startCropActivity(pickedImageUri)
                }
            }
        }

    }

    private fun startCropActivity(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .getIntent(this)

        cropImageLauncher.launch(cropIntent)
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
        val existingFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (existingFragment is FlutterFragment) {
            flutterFragment = existingFragment
        } else {
            flutterFragment = FlutterFragment.withCachedEngine(ENGINE_ID)
                .shouldAttachEngineToActivity(true)
                .build()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, flutterFragment!!, FRAGMENT_TAG)
                .commit()
        }
    }


    private fun setMethodChannels(flutterEngine: FlutterEngine) {
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler { call: MethodCall, result: Result ->
            try {
                when (call.method) {
                    "getEncrypted" -> {
                        val data = call.argument<String>("data").orEmpty()
                        result.success(AndroidCypher().encrypt(data))
                    }
                    "getDecrypted" -> {
                        val data = call.argument<String>("data").orEmpty()
                        result.success(AndroidCypher().decrypt(data))
                    }
                    "appSignature" -> {
                        val helper = AppSignatureHelper(this)
                        val otpAutoRefCode = helper.appSignatures.firstOrNull().orEmpty()
                        startSMSListener()
                        SmsBroadcastReceiver.callBack = this
                        result.success(otpAutoRefCode)
                    }
                    "deviceActivationKey" -> {
                        result.success("""{"encrToken":"cfFDD0zK+OBglUMVOjVUZuhplD5kFP4+WA2Xn/2NuBc="}""")
                    }
                    "getSecureAndroidID" -> {
                        var uniqueID = PrefHelper.getInstance(this).prefGetIsDeviceUniqueId()
                        if (uniqueID.isEmpty()) {
                            uniqueID = UUID.randomUUID().toString()
                            PrefHelper.getInstance(this).prefSetIsDeviceUniqueId(uniqueID)
                        }
                        result.success(uniqueID)
                    }
                    "getGalleryImage" -> {
                        pendingResult = result
                        if (hasCameraPermission()) {
                            pickImageFromGallery()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    "getCameraImage" -> {
                        pendingResult = result
                        if (hasCameraPermission()) {
                            launchCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    "finishActivity"->{
                        finish()
                    }
                    "sessionExpired"->{
                        val i = Intent(
                            this,
                            LoginActivity::class.java
                        )
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(i)
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result.error("ERROR", e.message, null)
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        }

        capturedImageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        captureImageLauncher.launch(takePictureIntent)
    }






    fun startSMSListener() {
        val mClient = SmsRetriever.getClient(this)
        val mTask = mClient.startSmsRetriever()
        mTask.addOnSuccessListener { /* SMS Retriever started */ }
        mTask.addOnFailureListener { /* SMS Retriever failed */ }
    }

    override fun receivedValue(value: String) {
        channel?.invokeMethod(
            "updateMessage",
            mapOf("message" to value)
        )
    }


    private fun pickImageFromGallery() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(pickImageIntent)
    }

    override fun onBackPressed() {
        if (flutterFragment != null) {
            flutterFragment!!.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }
}
