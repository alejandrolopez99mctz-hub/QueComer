package com.example.recetario.network

import com.example.recetario.model.MealResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApi {
    @GET("filter.php")
    fun obtenerPorPais(@Query("a") pais: String): Call<MealResponse>

    @GET("lookup.php")
    fun obtenerReceta(@Query("i") id: String): Call<MealResponse>
}