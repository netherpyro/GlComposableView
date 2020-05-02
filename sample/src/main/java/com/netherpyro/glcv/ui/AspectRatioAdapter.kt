package com.netherpyro.glcv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.netherpyro.glcv.AspectRatio
import com.netherpyro.glcv.R
import kotlinx.android.synthetic.main.i_aspect.view.*

/**
 * @author mmikhailov on 02.05.2020.
 */
class AspectRatioAdapter(
        private val items: List<AspectRatioItem>,
        itemClickListener: (AspectRatio) -> Unit
) : RecyclerView.Adapter<AspectRatioViewHolder>() {

    private val itemClickListenerInternal: (AspectRatio, Int) -> Unit = { ar, position ->
        items.forEachIndexed { idx, item ->
            val wasSelected = item.selected
            item.selected = idx == position
            if (item.selected != wasSelected) notifyItemChanged(idx)
        }

        itemClickListener(ar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AspectRatioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.i_aspect, parent, false)
        return AspectRatioViewHolder(view, itemClickListenerInternal)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AspectRatioViewHolder, position: Int) {
        holder.bind(items[position])
    }
}

class AspectRatioViewHolder(
        view: View,
        itemClickListener: (AspectRatio, Int) -> Unit
) : RecyclerView.ViewHolder(view) {

    private lateinit var item: AspectRatioItem

    init {
        itemView.setOnClickListener { itemClickListener(item.aspectRatio, adapterPosition) }
    }


    fun bind(item: AspectRatioItem) {
        this.item = item

        itemView.btn_aspect.text = item.title
        itemView.btn_aspect.isSelected = item.selected
    }
}

data class AspectRatioItem(
        val aspectRatio: AspectRatio,
        val title: String,
        var selected: Boolean
)