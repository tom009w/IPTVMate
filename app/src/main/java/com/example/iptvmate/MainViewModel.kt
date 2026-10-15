package com.example.iptvmate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _title = MutableLiveData<String>("IPTV Mate")
    val title: LiveData<String> get() = _title
}
