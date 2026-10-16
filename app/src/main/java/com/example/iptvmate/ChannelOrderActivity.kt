package com.example.iptvmate

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChannelOrderActivity : AppCompatActivity() {

    private lateinit var rvChannelOrder: RecyclerView
    private lateinit var rvCategorySelector: RecyclerView
    private lateinit var btnSaveOrder: Button
    private lateinit var repository: PlaylistRepository

    private val masterChannelList = mutableListOf<Channel>()
    private val displayedChannelList = mutableListOf<Channel>()
    private var selectedCategory: String = "All Channels"

    private lateinit var channelAdapter: ChannelOrderAdapter
    private lateinit var chipAdapter: CategoryChipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_order)

        rvChannelOrder = findViewById(R.id.rvChannelOrder)
        rvCategorySelector = findViewById(R.id.rvCategorySelector)
        btnSaveOrder = findViewById(R.id.btnSaveOrder)
        repository = PlaylistRepository(applicationContext)

        rvChannelOrder.layoutManager = LinearLayoutManager(this)
        rvChannelOrder.setHasFixedSize(true)
        rvChannelOrder.setItemViewCacheSize(20)

        rvCategorySelector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategorySelector.setHasFixedSize(true)

        masterChannelList.addAll(repository.loadCachedChannels())

        val categories = GroupExtractor.extractGroups(
            masterChannelList,
            repository.loadCategoryOrder(),
            repository.loadHiddenCategories()
        )

        chipAdapter = CategoryChipAdapter(categories, selectedCategory) { category ->
            syncDisplayedBackToMaster()
            selectedCategory = category
            refreshDisplayedChannels()
        }
        rvCategorySelector.adapter = chipAdapter

        refreshDisplayedChannels()

        channelAdapter = ChannelOrderAdapter(
            channels = displayedChannelList,
            onMoveUp = { pos ->
                if (pos > 0) {
                    val item = displayedChannelList.removeAt(pos)
                    displayedChannelList.add(pos - 1, item)
                    channelAdapter.notifyItemMoved(pos, pos - 1)
                    channelAdapter.notifyItemRangeChanged(pos - 1, 2)
                }
            },
            onMoveDown = { pos ->
                if (pos < displayedChannelList.size - 1) {
                    val item = displayedChannelList.removeAt(pos)
                    displayedChannelList.add(pos + 1, item)
                    channelAdapter.notifyItemMoved(pos, pos + 1)
                    channelAdapter.notifyItemRangeChanged(pos, 2)
                }
            },
            onFavoriteToggle = { _, _ ->
                // favorite toggled directly in displayedChannelList
            }
        )

        rvChannelOrder.adapter = channelAdapter

        btnSaveOrder.setOnClickListener {
            syncDisplayedBackToMaster()
            updateOrderIndices()
            repository.saveCachedChannels(masterChannelList)
            Toast.makeText(this, "Channel order & favorites saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun refreshDisplayedChannels() {
        displayedChannelList.clear()
        if (selectedCategory == "All Channels") {
            displayedChannelList.addAll(masterChannelList)
        } else {
            displayedChannelList.addAll(GroupExtractor.filterChannels(masterChannelList, selectedCategory))
        }
        if (::channelAdapter.isInitialized) {
            channelAdapter.notifyDataSetChanged()
        }
    }

    private fun syncDisplayedBackToMaster() {
        if (selectedCategory == "All Channels") {
            masterChannelList.clear()
            masterChannelList.addAll(displayedChannelList)
            return
        }
        val newMaster = mutableListOf<Channel>()
        var insertedFiltered = false

        for (ch in masterChannelList) {
            val title = ch.groupTitle?.trim().let { t -> if (t.isNullOrBlank()) "Uncategorized" else t }
            if (title == selectedCategory) {
                if (!insertedFiltered) {
                    newMaster.addAll(displayedChannelList)
                    insertedFiltered = true
                }
            } else {
                newMaster.add(ch)
            }
        }
        masterChannelList.clear()
        masterChannelList.addAll(newMaster)
    }

    private fun updateOrderIndices() {
        for (i in masterChannelList.indices) {
            masterChannelList[i] = masterChannelList[i].copy(orderIndex = i)
        }
    }
}
