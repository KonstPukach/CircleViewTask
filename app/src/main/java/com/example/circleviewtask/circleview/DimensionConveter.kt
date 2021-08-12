package com.example.circleviewtask.circleview

import android.content.res.Resources
import android.util.TypedValue

fun convertToPx(dp: Float, r: Resources?): Float {
    return r?.let {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            it.displayMetrics
        )
    } ?: dp
}

fun convertToDp (px: Float, r: Resources?): Float {
    return r?.let {
        px / r.displayMetrics.density
    } ?: px
}
