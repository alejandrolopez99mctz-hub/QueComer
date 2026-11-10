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
import androidx.appcompat.app.AlertDialog
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
    private var llamadaDetalle: Call<MealResponse>? = null
    private var dialogoDetalle: AlertDialog? = null

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
        dialogoDetalle?.dismiss()
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

    private fun mostrarDetalle(receta: Meal) {
        val id = receta.idMeal ?: return
        dialogoDetalle?.dismiss()
        val dialogo = AlertDialog.Builder(this)
            .setTitle(receta.strMeal ?: getString(R.string.receta_sin_nombre))
            .setMessage(R.string.cargando_detalle)
            .setPositiveButton(R.string.reintentar, null)
            .setNegativeButton(R.string.cerrar, null)
            .create()
        dialogoDetalle = dialogo
        dialogo.setOnDismissListener {
            llamadaDetalle?.cancel()
            llamadaDetalle = null
            dialogoDetalle = null
        }
        dialogo.show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { cargarDetalle(id, dialogo) }
        cargarDetalle(id, dialogo)
    }

    // filter.php sólo incluye nombre, ID y foto; lookup.php aporta ingredientes y pasos.
    private fun cargarDetalle(id: String, dialogo: AlertDialog) {
        llamadaDetalle?.cancel()
        dialogo.setMessage(getString(R.string.cargando_detalle))
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.GONE
        val peticion = RetrofitClient.api.obtenerReceta(id)
        llamadaDetalle = peticion
        peticion.enqueue(object : Callback<MealResponse> {
            override fun onResponse(call: Call<MealResponse>, response: Response<MealResponse>) {
                if (llamadaDetalle !== call || isDestroyed || !dialogo.isShowing) return
                val receta = response.body()?.meals?.firstOrNull()
                if (!response.isSuccessful || receta == null) {
                    dialogo.setMessage(if (response.isSuccessful) getString(R.string.detalle_no_disponible)
                        else getString(R.string.error_servidor, response.code()))
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
                    return
                }
                val ingredientes = receta.ingredientes().joinToString("\n") { "• $it" }
                    .ifBlank { getString(R.string.sin_ingredientes) }
                val preparacion = receta.strInstructions?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.sin_preparacion)
                val origen = listOfNotNull(receta.strCategory, receta.strArea).joinToString(" · ")
                dialogo.setMessage("$origen\n\n${getString(R.string.ingredientes)}\n$ingredientes\n\n" +
                    "${getString(R.string.preparacion)}\n$preparacion")
            }

            override fun onFailure(call: Call<MealResponse>, error: Throwable) {
                if (call.isCanceled || llamadaDetalle !== call || isDestroyed || !dialogo.isShowing) return
                dialogo.setMessage(getString(R.string.error_recetas))
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).visibility = View.VISIBLE
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("area", paisActual.area)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        llamadaLista?.cancel()
        dialogoDetalle?.dismiss()
        llamadaDetalle?.cancel()
        super.onDestroy()
    }
}