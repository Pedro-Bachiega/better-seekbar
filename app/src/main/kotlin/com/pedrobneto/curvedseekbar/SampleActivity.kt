package com.pedrobneto.curvedseekbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.pedrobneto.bar.CurvedSeekBar

class SampleActivity : AppCompatActivity() {

    private lateinit var seekBar: CurvedSeekBar

    private val preferredPointBySegment = mapOf(0 to 2, 1 to 8, 2 to 11)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val totalPoints = 0

        val topTextView = findViewById<TextView>(R.id.top_text_view)
        val bottomTextView = findViewById<TextView>(R.id.bottom_text_view)
        topTextView.isVisible = totalPoints > 0

        topTextView.text = "Points: $totalPoints"

        seekBar = findViewById(R.id.seek_bar)
        seekBar.setOnPointSelectedUpdated { bottomTextView.text = "Selected point: ${it + 1}" }

        if (totalPoints > 0) {
            seekBar.pointQuantity = totalPoints
            seekBar.segmentQuantity = 5
            seekBar.setPreferredPointOnClickBySegment(preferredPointBySegment)
        } else {
            seekBar.setOnProgressUpdatedListener { bottomTextView.text = "Progress: ${it * 100}%" }
            seekBar.setOnProgressStopChangingListener {
                Toast.makeText(this, "Progress: ${it * 100}%", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        seekBar.doOnPreDraw { seekBar.setSelectedPoint(preferredPointBySegment[1]!!) }
//    }
}