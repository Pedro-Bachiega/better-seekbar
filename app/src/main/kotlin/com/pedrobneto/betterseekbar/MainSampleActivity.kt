package com.pedrobneto.betterseekbar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainSampleActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.better_bar).setOnClickListener {
            startSample<BetterSeekBarSampleActivity>()
        }

        findViewById<View>(R.id.curved_bar).setOnClickListener {
            startSample<CurvedSeekBarSampleActivity>()
        }
    }

    private inline fun <reified T : AppCompatActivity> startSample() =
        startActivity(Intent(this, T::class.java))
}