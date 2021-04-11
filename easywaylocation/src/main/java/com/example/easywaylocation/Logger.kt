package com.example.easywaylocation

import android.util.Log

object Logger {
     var isDebuggable = false;

    fun LogDebug(tag:String,mess:String){
       if (isDebuggable){
           Log.e(tag,mess)
       }
    }

    fun LogInfo(tag:String,mess:String){
        if (isDebuggable){
            Log.v(tag,mess)
        }
    }
}