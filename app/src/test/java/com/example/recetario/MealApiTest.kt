package com.example.recetario

import com.example.recetario.model.MealResponse
import com.example.recetario.network.RetrofitClient
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealApiTest {
    @Test
    fun ingredientesConservanSuMedidaYOmiteCamposVacios() {
        val json = """{"meals":[{"idMeal":"1","strMeal":"Sopa",
            "strIngredient1":" Pollo ","strMeasure1":" 200 g ",
            "strIngredient2":" ","strMeasure2":"1",
            "strIngredient3":"Sal","strMeasure3":null,
            "strIngredient20":"Agua","strMeasure20":"2 tazas"}]}"""
        val receta = Gson().fromJson(json, MealResponse::class.java).meals!!.single()
        assertEquals(listOf("200 g Pollo", "Sal", "2 tazas Agua"), receta.ingredientes())
    }

    @Test
    fun respuestaSinResultadosAdmiteMealsNull() {
        assertNull(Gson().fromJson("""{"meals":null}""", MealResponse::class.java).meals)
    }

    @Test
    fun resumenDeFiltroNoRequiereCamposDeDetalle() {
        val receta = Gson().fromJson(
            """{"meals":[{"idMeal":"52772","strMeal":"Chicken","strMealThumb":"https://example.com/meal.jpg"}]}""",
            MealResponse::class.java
        ).meals!!.single()
        assertEquals("52772", receta.idMeal)
        assertEquals(emptyList<String>(), receta.ingredientes())
        assertNull(receta.strInstructions)
    }

    @Test
    fun peticionesUsanLaBaseYParametrosDeTheMealDb() {
        assertEquals("https://www.themealdb.com/api/json/v1/1/filter.php?a=Mexican",
            RetrofitClient.api.obtenerPorPais("Mexican").request().url.toString())
        assertEquals("https://www.themealdb.com/api/json/v1/1/lookup.php?i=52772",
            RetrofitClient.api.obtenerReceta("52772").request().url.toString())
    }
}