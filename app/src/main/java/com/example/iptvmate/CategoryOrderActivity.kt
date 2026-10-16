package com.example.iptvmate

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryOrderActivity : AppCompatActivity() {

    private lateinit var rvCategoryOrder: RecyclerView
    private lateinit var btnSaveCategoryOrder: Button
    private lateinit var repository: PlaylistRepository
    private val categoryList = mutableListOf<String>()
    private val hiddenCategories = mutableSetOf<String>()
    private lateinit var adapter: CategoryOrderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_order)

        rvCategoryOrder = findViewById(R.id.rvCategoryOrder)
        btnSaveCategoryOrder = findViewById(R.id.btnSaveCategoryOrder)
        repository = PlaylistRepository(applicationContext)

        rvCategoryOrder.layoutManager = LinearLayoutManager(this)
        rvCategoryOrder.setHasFixedSize(true)
        rvCategoryOrder.setItemViewCacheSize(20)

        val channels = repository.loadCachedChannels()
        val config = repository.loadCategoryConfig()
        hiddenCategories.addAll(config.hidden)

        // Extract all groups including hidden ones so user can unhide them
        val allExtracted = GroupExtractor.extractGroups(channels, config.order, emptyList())
        categoryList.addAll(allExtracted.filter { it != "All Channels" })

        adapter = CategoryOrderAdapter(
            categories = categoryList,
            hiddenCategories = hiddenCategories,
            onMoveUp = { pos ->
                if (pos > 0) {
                    val item = categoryList.removeAt(pos)
                    categoryList.add(pos - 1, item)
                    adapter.notifyItemMoved(pos, pos - 1)
                    adapter.notifyItemRangeChanged(pos - 1, 2)
                }
            },
            onMoveDown = { pos ->
                if (pos < categoryList.size - 1) {
                    val item = categoryList.removeAt(pos)
                    categoryList.add(pos + 1, item)
                    adapter.notifyItemMoved(pos, pos + 1)
                    adapter.notifyItemRangeChanged(pos, 2)
                }
            }
        )

        rvCategoryOrder.adapter = adapter

        btnSaveCategoryOrder.setOnClickListener {
            repository.saveCategoryConfig(
                PlaylistRepository.CategoryConfig(
                    order = categoryList,
                    hidden = hiddenCategories.toList()
                )
            )
            Toast.makeText(this, "Category settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
