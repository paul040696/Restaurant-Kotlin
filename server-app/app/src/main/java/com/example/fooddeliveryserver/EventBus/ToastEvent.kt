package com.example.fooddeliveryserver.EventBus

import com.example.fooddeliveryserver.Common.Common

class ToastEvent(var action:Common.ACTION, var isBackFromFoodList: Boolean) {
}