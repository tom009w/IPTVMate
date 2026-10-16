package com.example.iptvmate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryChipAdapter(
    private val categories: List<String>,
    private var selectedCategory: String,
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder>() {

    class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryChip: TextView = itemView.findViewById(R.id.tvCategoryChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryChip.text = category

        val isSelected = category == selectedCategory
        if (isSelected) {
            holder.tvCategoryChip.setBackgroundColor(Color.parseColor("#00E5FF"))
            holder.tvCategoryChip.setTextColor(Color.BLACK)
        } else {
            holder.tvCategoryChip.setBackgroundResource(R.drawable.tv_focusable_background)
            holder.tvCategoryChip.setTextColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            selectedCategory = category
            notifyDataSetChanged()
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}
