package com.example.nexus

import android.app.Application
import com.example.nexus.di.NexusAppContainer

class NexusApplication : Application() {
    lateinit var container: NexusAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = NexusAppContainer(this)
    }
}
