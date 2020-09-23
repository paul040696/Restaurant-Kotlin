package com.example.fooddeliveryserver.Callback

import com.example.fooddeliveryserver.Model.OrderModel

interface IOrderCallbackListener {

    fun onOrderLoadSuccess(orderModel: List<OrderModel>)
    fun onOrderLoadFailed(message:String)

}