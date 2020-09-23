package com.example.fooddeliveryserver.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fooddeliveryserver.Callback.ICategoryCallbackListener
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.Model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoryViewModel : ViewModel(), ICategoryCallbackListener {
    override fun onCategoryLoadFailed(message: String) {
        messageError.value = message
    }

    override fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>) {
        categoriesListMutable!!.value = categoriesList
    }

    private var categoriesListMutable: MutableLiveData<List<CategoryModel>>?=null
    private var messageError:MutableLiveData<String> = MutableLiveData()
    private val categoryCallBackListener: ICategoryCallbackListener

    init {
        categoryCallBackListener = this
    }

    fun getCategoryList():MutableLiveData<List<CategoryModel>>{
        if(categoriesListMutable == null)
        {
            categoriesListMutable = MutableLiveData()
            loadCategory()
        }
        return  categoriesListMutable!!
    }

    fun getMessageError(): MutableLiveData<String>{
        return messageError
    }

    fun loadCategory() {

        val tempList = ArrayList<CategoryModel>()
        val categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
        categoryRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                categoryCallBackListener.onCategoryLoadFailed(p0.message!!)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for(itemSnapshot in p0.children)
                {
                    val model = itemSnapshot.getValue<CategoryModel>(CategoryModel::class.java)
                    model!!.menu_id = itemSnapshot.key
                    tempList.add(model)
                }
                categoryCallBackListener.onCategoryLoadSuccess(tempList)
            }
        })
    }
}