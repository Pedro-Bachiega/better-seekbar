package com.pedrobneto.betterseekbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.pedrobneto.bar.BetterSeekBar

open class BetterSeekBarSampleActivity : AppCompatActivity() {

    private lateinit var seekBar: BetterSeekBar

    //    private val preferredPointBySegment = mapOf(0 to 11, 1 to 33, 2 to 55)
    private val preferredPointBySegment = mapOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5)

    protected open val layoutRes: Int = R.layout.activity_better_seek_bar

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)

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
            seekBar.setStartingPoint(preferredPointBySegment[1]!!)
        } else {
            seekBar.setOnProgressUpdatedListener { bottomTextView.text = "Progress: ${it * 100}%" }
            seekBar.setOnProgressStopChangingListener {
                Toast.makeText(this, "Progress: ${it * 100}%", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.previous).setOnClickListener { seekBar.setSelectedPoint(seekBar.lastPointSelected - 1) }
        findViewById<View>(R.id.next).setOnClickListener { seekBar.setSelectedPoint(seekBar.lastPointSelected + 1) }
    }
}