package com.example.fooddeliveryfinal.Callback

import com.example.fooddeliveryfinal.Model.PopularCategoryModel

interface IPopularLoadCallback {
    fun onPopularLoadSuccess(popularModelList: List<PopularCategoryModel>)
    fun onPopularLoadFailed(message:String)
}