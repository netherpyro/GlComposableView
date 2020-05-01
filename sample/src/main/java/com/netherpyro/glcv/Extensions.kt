package com.netherpyro.glcv

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.AttrRes

/**
 * @author mmikhailov on 01.05.2020.
 */
fun <T : View> T.alsoOnLaid(block: (T) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            block.invoke(this@alsoOnLaid)
        }
    })
}

fun Context.attrValue(@AttrRes res: Int): Int {
    val value: Int?
    val tv = TypedValue()
    if (this.theme.resolveAttribute(res, tv, true)) value = tv.data
    else throw Resources.NotFoundException("Resource with id $res not found")
    return value
}