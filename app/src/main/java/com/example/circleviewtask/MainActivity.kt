package com.example.circleviewtask

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.circleviewtask.circleview.CircleView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val circleView: CircleView = findViewById(R.id.circle_view)
        val btnPartsAmount: Button = findViewById(R.id.btn_change_part_amount)
        val btnColor: Button = findViewById(R.id.btn_change_color)
        val seekBarChangeRadius: SeekBar = findViewById(R.id.seek_bar_change_radius)

        circleView.items = listOf(
            CircleView.Item(R.drawable.icon_sun),
            CircleView.Item(R.drawable.ic_web),
            CircleView.Item(R.drawable.ic_baseline_brightness_low_24),
            CircleView.Item(R.drawable.ic_baseline_brightness_high_24),
            CircleView.Item(R.drawable.ic_baseline_brightness_6_24)
        )

        btnPartsAmount.setOnClickListener {
            circleView.addItem(CircleView.Item(R.drawable.ic_baseline_brightness_1_24))
        }

        val rand = Random(255)
        btnColor.setOnClickListener {
            circleView.circleColor = Color.argb(
                255,
                rand.nextInt(256),
                rand.nextInt(256),
                rand.nextInt(256)
            )
        }

        circleView.setOnClickListener {
            Log.d("Activity", circleView.items.toString())
        }

        seekBarChangeRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                circleView.radius = p1.toFloat()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }
}