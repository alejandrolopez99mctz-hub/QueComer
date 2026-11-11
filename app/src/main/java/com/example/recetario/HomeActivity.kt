package com.example.recetario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.recetario.model.Meal
import com.example.recetario.model.MealResponse
import com.example.recetario.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {
    private data class Pais(val boton: Int, val nombre: String, val area: String)

    // TheMealDB recibe el nombre de la cocina en inglés, no el país en español.
    private val paises = listOf(
        Pais(R.id.btnChina, "China", "Chinese"),
        Pais(R.id.btnArgentina, "Argentina", "Argentine"),
        Pais(R.id.btnItalia, "Italia", "Italian"),
        Pais(R.id.btnAlemania, "Alemania", "German"),
        Pais(R.id.btnJapon, "Japón", "Japanese"),
        Pais(R.id.btnMexico, "México", "Mexican"),
        Pais(R.id.btnColombia, "Colombia", "Colombian"),
        Pais(R.id.btnEspana, "España", "Spanish")
    )
    private lateinit var paisActual: Pais
    private lateinit var tvEstado: TextView
    private lateinit var progreso: ProgressBar
    private lateinit var btnReintentar: Button
    private lateinit var contenedor: LinearLayout
    private var llamadaLista: Call<MealResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        tvEstado = findViewById(R.id.tvEstado)
        progreso = findViewById(R.id.progresoRecetas)
        btnReintentar = findViewById(R.id.btnReintentar)
        contenedor = findViewById(R.id.contenedorRecetas)

        findViewById<LinearLayout>(R.id.btnPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
        paises.forEach { pais ->
            findViewById<Button>(pais.boton).setOnClickListener { obtenerRecetas(pais) }
        }
        btnReintentar.setOnClickListener { obtenerRecetas(paisActual) }
        findViewById<LinearLayout>(R.id.btnExplorar).setOnClickListener { obtenerRecetas(paisActual) }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        val areaGuardada = savedInstanceState?.getString("area")
        obtenerRecetas(paises.firstOrNull { it.area == areaGuardada } ?: paises[5])
    }

    private fun obtenerRecetas(pais: Pais) {
        llamadaLista?.cancel()
        paisActual = pais
        paises.forEach {
            findViewById<Button>(it.boton).apply {
                isSelected = it == pais
                alpha = if (it == pais) 1f else 0.65f
            }
        }
        findViewById<TextView>(R.id.tvTituloRecetas).text = getString(R.string.recetas_de, pais.nombre)
        contenedor.removeAllViews()
        tvEstado.setText(R.string.cargando_recetas)
        progreso.visibility = View.VISIBLE
        btnReintentar.visibility = View.GONE

        val peticion = RetrofitClient.api.obtenerPorPais(pais.area)
        llamadaLista = peticion
        peticion.enqueue(object : Callback<MealResponse> {
            override fun onResponse(call: Call<MealResponse>, response: Response<MealResponse>) {
                if (llamadaLista !== call || isDestroyed) return
                progreso.visibility = View.GONE
                if (!response.isSuccessful) {
                    mostrarError(getString(R.string.error_servidor, response.code()))
                    return
                }
                val recetas = response.body()?.meals.orEmpty().filter { !it.idMeal.isNullOrBlank() }
                if (recetas.isEmpty()) {
                    tvEstado.setText(R.string.sin_recetas)
                } else {
                    tvEstado.text = getString(R.string.recetas_encontradas, recetas.size)
                    recetas.forEach(::agregarReceta)
                }
            }

            override fun onFailure(call: Call<MealResponse>, error: Throwable) {
                if (call.isCanceled || llamadaLista !== call || isDestroyed) return
                mostrarError(getString(R.string.error_recetas))
            }
        })
    }

    private fun mostrarError(mensaje: String) {
        progreso.visibility = View.GONE
        tvEstado.text = mensaje
        btnReintentar.visibility = View.VISIBLE
    }

    private fun agregarReceta(receta: Meal) {
        val tarjeta = layoutInflater.inflate(R.layout.item_receta, contenedor, false)
        val nombre = receta.strMeal ?: getString(R.string.receta_sin_nombre)
        tarjeta.findViewById<TextView>(R.id.tvNombreReceta).text = nombre
        val imagen = tarjeta.findViewById<ImageView>(R.id.imgReceta)
        imagen.contentDescription = getString(R.string.foto_receta_nombre, nombre)
        Glide.with(this)
            .load(receta.strMealThumb)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .centerCrop()
            .into(imagen)
        tarjeta.setOnClickListener { mostrarDetalle(receta) }
        contenedor.addView(tarjeta)
    }

    // Igual que MusicApp: la tarjeta envía los datos de la API a otra Activity.
    private fun mostrarDetalle(receta: Meal) {
        val id = receta.idMeal?.takeIf { it.isNotBlank() } ?: return
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra(DetailsActivity.EXTRA_ID_RECETA, id)
            putExtra(DetailsActivity.EXTRA_NOMBRE_RECETA, receta.strMeal)
            putExtra(DetailsActivity.EXTRA_IMAGEN_RECETA, receta.strMealThumb)
        }
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("area", paisActual.area)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        llamadaLista?.cancel()
        super.onDestroy()
    }
}
