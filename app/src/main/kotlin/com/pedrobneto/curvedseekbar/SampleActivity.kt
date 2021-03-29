package com.pedrobneto.curvedseekbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.pedrobneto.bar.CurvedSeekBar

class SampleActivity : AppCompatActivity() {

    private lateinit var seekBar: CurvedSeekBar

    private val preferredPointBySegment = mapOf(0 to 2, 1 to 8, 2 to 11)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val totalPoints = 15

        val topTextView = findViewById<TextView>(R.id.top_text_view)
        val bottomTextView = findViewById<TextView>(R.id.bottom_text_view)

        topTextView.text = "Points: $totalPoints"

        seekBar = findViewById(R.id.seek_bar)
        seekBar.pointQuantity = totalPoints
        seekBar.setOnPointSelectedUpdated { bottomTextView.text = "Selected point: ${it + 1}" }
        seekBar.setPreferredPointOnClickBySegment(preferredPointBySegment)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        seekBar.doOnPreDraw { seekBar.setSelectedPoint(preferredPointBySegment[1]!!) }
    }
}