package io.jans.chip.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiAdapter {
    fun getInstance(baseUrl: String?): APIInterface {
        var apiInterface: APIInterface? = null
        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiInterface = retrofit.create(APIInterface::class.java)
        return apiInterface
    }
}