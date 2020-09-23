package com.example.fooddeliveryserver.Callback

import com.example.fooddeliveryserver.Model.CategoryModel

interface ICategoryCallbackListener {
    fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}