package com.example.loanappandroid

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.loanappandroid.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        // Fontapp.getApp().getAppServiceComponent().inject(this)

        binding.btnEkyc.setOnClickListener {
            startActivity(Intent(this, KycActivity::class.java))
        }
    }
}
