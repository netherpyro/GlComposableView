package com.netherpyro.glcv

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

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

fun Context.dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.coerceAtLeast(0f), resources.displayMetrics)

fun Context.getActionBarSize(): Int {
    var actionBarHeight = 0
    val typedValue = TypedValue()

    try {
        if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        }
    } catch (ignore: Exception) {
    }

    if (actionBarHeight == 0) {
        actionBarHeight = dpToPx(52f).toInt()
    }

    return actionBarHeight
}

fun RecyclerView.addDivider() {
    val decoration =
            DividerItemDecoration(context, LinearLayout.HORIZONTAL)
                .apply { setDrawable(ContextCompat.getDrawable(context, R.drawable.shape_divider)!!) }
    addItemDecoration(decoration)
}