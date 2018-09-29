package com.edok.v4n0v.edok

import android.app.AppComponentFactory
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

open class BaseActivity:AppCompatActivity(){


    fun makeLog(msg:String){
        Log.d(this.javaClass.simpleName, msg)
    }
    fun makeLog(exception:Exception){
        Log.e(this.javaClass.simpleName, exception.message.toString())
    }
//    fun info(msg:String){
//        Log.i(this.javaClass.simpleName, msg)
//    }

    fun toast(msg:String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}