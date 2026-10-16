package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryOrderAdapter(
    private val categories: MutableList<String>,
    private val hiddenCategories: MutableSet<String>,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit
) : RecyclerView.Adapter<CategoryOrderAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryOrderNumber: TextView = itemView.findViewById(R.id.tvCategoryOrderNumber)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val switchCategoryVisible: SwitchCompat = itemView.findViewById(R.id.switchCategoryVisible)
        val btnCategoryMoveUp: Button = itemView.findViewById(R.id.btnCategoryMoveUp)
        val btnCategoryMoveDown: Button = itemView.findViewById(R.id.btnCategoryMoveDown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryOrderNumber.text = (position + 1).toString()
        holder.tvCategoryName.text = category

        holder.switchCategoryVisible.setOnCheckedChangeListener(null)
        holder.switchCategoryVisible.isChecked = !hiddenCategories.contains(category)

        holder.switchCategoryVisible.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hiddenCategories.remove(category)
            } else {
                hiddenCategories.add(category)
            }
        }

        holder.btnCategoryMoveUp.setOnClickListener {
            onMoveUp(position)
        }

        holder.btnCategoryMoveDown.setOnClickListener {
            onMoveDown(position)
        }
    }

    override fun getItemCount(): Int = categories.size
}
