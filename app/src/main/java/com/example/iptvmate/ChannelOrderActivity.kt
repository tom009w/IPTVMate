package com.example.iptvmate

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
            onChannelClick = { pos ->
                showChannelActionDialog(pos)
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

    private fun showChannelActionDialog(pos: Int) {
        val channel = displayedChannelList[pos]
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (pos > 0) {
            options.add("⬆️ Move Up")
            actions.add {
                displayedChannelList.removeAt(pos)
                displayedChannelList.add(pos - 1, channel)
                syncDisplayedBackToMaster()
                channelAdapter.notifyDataSetChanged()
                rvChannelOrder.smoothScrollToPosition(pos - 1)
                rvChannelOrder.post {
                    rvChannelOrder.findViewHolderForAdapterPosition(pos - 1)?.itemView?.requestFocus()
                }
            }
        }

        if (pos < displayedChannelList.size - 1) {
            options.add("⬇️ Move Down")
            actions.add {
                displayedChannelList.removeAt(pos)
                displayedChannelList.add(pos + 1, channel)
                syncDisplayedBackToMaster()
                channelAdapter.notifyDataSetChanged()
                rvChannelOrder.smoothScrollToPosition(pos + 1)
                rvChannelOrder.post {
                    rvChannelOrder.findViewHolderForAdapterPosition(pos + 1)?.itemView?.requestFocus()
                }
            }
        }

        val favText = if (channel.isFavorite) "⭐ Remove from Favorites" else "⭐ Add to Favorites"
        options.add(favText)
        actions.add {
            val newFav = !channel.isFavorite
            displayedChannelList[pos] = channel.copy(isFavorite = newFav)
            syncDisplayedBackToMaster()
            channelAdapter.notifyItemChanged(pos)
            rvChannelOrder.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
        }

        options.add("❌ Cancel")

        AlertDialog.Builder(this)
            .setTitle(channel.name)
            .setItems(options.toTypedArray()) { _, which ->
                if (which in actions.indices) {
                    actions[which].invoke()
                }
            }
            .show()
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
