package com.example.circleviewtask.circleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.scale


// Build Bitmap instance from a drawable icon id
fun getBitmap(context: Context, icon: Int, iconSize: Int): Bitmap? {
    val drawable = AppCompatResources.getDrawable(context, icon)
    if (drawable is BitmapDrawable) {
        return drawable.bitmap.scale(iconSize, iconSize)
    } else {
        drawable?.let { dr ->
            val bitmap = Bitmap.createBitmap(
                iconSize,
                iconSize,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            dr.setBounds(0, 0, canvas.width, canvas.height)
            dr.draw(canvas)
            return bitmap
        }
    }
    return null
}