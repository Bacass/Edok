package com.edok.v4n0v.edok

import android.app.Application
import android.util.Log
import android.widget.Toast

class App: Application(){
    private lateinit var instance: App
    companion object {




    }

    override fun onCreate() {
        super.onCreate()
        instance=this
    }

    fun getInstance():App{
        return instance
    }
}