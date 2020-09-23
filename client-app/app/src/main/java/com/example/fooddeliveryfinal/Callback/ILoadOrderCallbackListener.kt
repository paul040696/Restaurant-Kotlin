package com.example.fooddeliveryfinal.Callback

import com.example.fooddeliveryfinal.Model.Order

interface ILoadOrderCallbackListener {
    fun onLoadOrderSuccess(orderList:List<Order>)
    fun onLoadOrderFailed(message:String)
}