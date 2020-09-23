package com.example.fooddeliveryfinal.Remote

import okhttp3.internal.Internal.instance
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitCloudClient {

    private var instance:Retrofit?=null

    fun getInstance(): Retrofit {
        if (instance  == null)
            instance = Retrofit.Builder()
                .baseUrl("https://us-central1-food-delivery-final-bb6cb.cloudfunctions.net/widgeets/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        return instance!!
    }
}