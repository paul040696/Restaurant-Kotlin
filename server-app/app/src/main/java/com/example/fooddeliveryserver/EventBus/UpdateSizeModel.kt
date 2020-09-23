package com.example.fooddeliveryserver.EventBus

import com.example.fooddeliveryserver.Model.SizeModel

class UpdateSizeModel {
    var sizeModelList: List<SizeModel>?=null
    constructor(){}
    constructor(sizeModelList:List<SizeModel>?){
        this.sizeModelList = sizeModelList
    }


}
