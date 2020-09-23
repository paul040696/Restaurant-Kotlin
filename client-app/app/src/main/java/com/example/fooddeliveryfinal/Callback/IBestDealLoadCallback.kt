package com.example.fooddeliveryfinal.Callback

import com.example.fooddeliveryfinal.Model.BestDealModel
import com.example.fooddeliveryfinal.Model.PopularCategoryModel

interface IBestDealLoadCallback {
    fun onBestDealLoadSuccess(bestDealList: List<BestDealModel>)
    fun onBestDealLoadFailed(message:String)

}