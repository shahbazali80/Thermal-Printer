package com.example.thermalprinter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.thermalprinter.R
import android.content.Intent
import android.os.Handler
import com.example.thermalprinter.MainActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        Handler().postDelayed({
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }, 5000)
    }
}