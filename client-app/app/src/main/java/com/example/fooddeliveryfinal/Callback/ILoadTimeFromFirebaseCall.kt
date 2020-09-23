package com.example.fooddeliveryfinal.Callback

import androidx.room.FtsOptions
import com.example.fooddeliveryfinal.Model.Order

interface ILoadTimeFromFirebaseCall {
    fun onLoadTimeSucces(order: Order,estimatedTimeMs: Long)
    fun onLoadTimeFailed(message:String)
}