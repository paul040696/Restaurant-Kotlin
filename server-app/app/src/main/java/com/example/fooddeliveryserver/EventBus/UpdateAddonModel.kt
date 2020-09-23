package com.example.fooddeliveryserver.EventBus

import com.example.fooddeliveryserver.Model.AddonModel

class UpdateAddonModel {
    var addonModelList: List<AddonModel>?=null
    constructor(){}
    constructor(addonModelList:List<AddonModel>?){
        this.addonModelList = addonModelList
    }


}
