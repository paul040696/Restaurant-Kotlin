package com.example.fooddeliveryfinal.Callback

import com.example.fooddeliveryfinal.Model.CategoryModel
import com.example.fooddeliveryfinal.Model.PopularCategoryModel

interface ICategoryCallbackListener {
    fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}