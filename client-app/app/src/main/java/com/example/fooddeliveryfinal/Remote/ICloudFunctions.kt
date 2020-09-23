package com.example.fooddeliveryfinal.Remote

import com.example.fooddeliveryfinal.Model.BraintreeToken
import com.example.fooddeliveryfinal.Model.BraintreeTransaction
import io.reactivex.Observable
import retrofit2.http.*
import java.time.temporal.TemporalAmount
import java.util.*


interface ICloudFunctions {

    @GET("token")
    fun getToken(@HeaderMap headers:Map<String,String>): Observable<BraintreeToken>

    @POST("checkout")
    @FormUrlEncoded
    fun submitPayment(
        @HeaderMap headers:Map<String,String>,
        @Field("amount") amount: Double,
        @Field("payment_method_nonce") nonce:String):Observable<BraintreeTransaction>
}