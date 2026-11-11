package com.example.recetario

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

class DetailsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID_RECETA = "ID_RECETA"
        const val EXTRA_NOMBRE_RECETA = "NOMBRE_RECETA"
        const val EXTRA_IMAGEN_RECETA = "IMAGEN_RECETA"
    }

    private lateinit var tvEstado: TextView
    private lateinit var progreso: ProgressBar
    private lateinit var btnReintentar: Button
    private lateinit var contenido: View
    private lateinit var btnFavorito: ImageButton
    private var recetaFavorita = false
    private var llamadaDetalle: Call<MealResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        tvEstado = findViewById(R.id.tvEstadoDetalle)
        progreso = findViewById(R.id.progresoDetalle)
        btnReintentar = findViewById(R.id.btnReintentarDetalle)
        contenido = findViewById(R.id.contenidoDetalle)
        btnFavorito = findViewById(R.id.btnFavorito)
        recetaFavorita = savedInstanceState?.getBoolean("recetaFavorita") ?: false
        actualizarFavorito()
        findViewById<Button>(R.id.btnVolverRecetas).setOnClickListener { finish() }

        // Recibe nombre e imagen mediante Intent, como DetailsActivity de MusicApp.
        mostrarCabecera(intent.getStringExtra(EXTRA_NOMBRE_RECETA),
            intent.getStringExtra(EXTRA_IMAGEN_RECETA))
        val id = intent.getStringExtra(EXTRA_ID_RECETA)?.takeIf { it.isNotBlank() }
        if (id == null) {
            btnFavorito.isEnabled = false
            mostrarError(getString(R.string.detalle_no_disponible), permitirReintento = false)
            return
        }
        // Misma lógica de MusicApp: alternar estado e icono, sin escribir en una BD.
        btnFavorito.setOnClickListener {
            recetaFavorita = !recetaFavorita
            actualizarFavorito()
            val nombre = findViewById<TextView>(R.id.tvNombreDetalle).text
            Toast.makeText(this, getString(if (recetaFavorita) R.string.receta_agregada_favoritos
                else R.string.receta_eliminada_favoritos, nombre), Toast.LENGTH_SHORT).show()
        }
        btnReintentar.setOnClickListener { cargarDetalle(id) }
        cargarDetalle(id)
    }

    private fun actualizarFavorito() {
        btnFavorito.setImageResource(if (recetaFavorita) R.drawable.favorito else R.drawable.no_favorito)
        btnFavorito.isSelected = recetaFavorita
        btnFavorito.contentDescription = getString(if (recetaFavorita)
            R.string.quitar_favorito else R.string.agregar_favorito)
        findViewById<TextView>(R.id.tvEstadoFavorito).setText(if (recetaFavorita)
            R.string.favorito_temporal else R.string.agregar_favorito)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("recetaFavorita", recetaFavorita)
        super.onSaveInstanceState(outState)
    }

    private fun mostrarCabecera(nombre: String?, imagen: String?) {
        val titulo = nombre?.takeIf { it.isNotBlank() } ?: getString(R.string.receta_sin_nombre)
        findViewById<TextView>(R.id.tvNombreDetalle).text = titulo
        val foto = findViewById<ImageView>(R.id.imgDetalleReceta)
        foto.contentDescription = getString(R.string.foto_receta_nombre, titulo)
        Glide.with(this).load(imagen).placeholder(R.drawable.logo)
            .error(R.drawable.logo).centerCrop().into(foto)
    }

    // filter.php sólo incluye nombre, ID y foto; lookup.php aporta ingredientes y pasos.
    private fun cargarDetalle(id: String) {
        llamadaDetalle?.cancel()
        contenido.visibility = View.GONE
        tvEstado.setText(R.string.cargando_detalle)
        tvEstado.visibility = View.VISIBLE
        progreso.visibility = View.VISIBLE
        btnReintentar.visibility = View.GONE
        val peticion = RetrofitClient.api.obtenerReceta(id)
        llamadaDetalle = peticion
        peticion.enqueue(object : Callback<MealResponse> {
            override fun onResponse(call: Call<MealResponse>, response: Response<MealResponse>) {
                if (llamadaDetalle !== call || isDestroyed || isFinishing) return
                val receta = response.body()?.meals?.firstOrNull()
                if (!response.isSuccessful || receta == null) {
                    mostrarError(if (response.isSuccessful) getString(R.string.detalle_no_disponible)
                        else getString(R.string.error_servidor, response.code()))
                    return
                }
                mostrarReceta(receta)
            }

            override fun onFailure(call: Call<MealResponse>, error: Throwable) {
                if (call.isCanceled || llamadaDetalle !== call || isDestroyed || isFinishing) return
                mostrarError(getString(R.string.error_recetas))
            }
        })
    }

    private fun mostrarReceta(receta: Meal) {
        mostrarCabecera(receta.strMeal ?: intent.getStringExtra(EXTRA_NOMBRE_RECETA),
            receta.strMealThumb ?: intent.getStringExtra(EXTRA_IMAGEN_RECETA))
        findViewById<TextView>(R.id.tvOrigenDetalle).apply {
            text = listOfNotNull(receta.strCategory, receta.strArea)
                .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
            visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.tvIngredientesDetalle).text =
            receta.ingredientes().joinToString("\n") { "• $it" }
                .ifBlank { getString(R.string.sin_ingredientes) }
        findViewById<TextView>(R.id.tvPreparacionDetalle).text =
            receta.strInstructions?.trim()?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.sin_preparacion)
        progreso.visibility = View.GONE
        tvEstado.visibility = View.GONE
        btnReintentar.visibility = View.GONE
        contenido.visibility = View.VISIBLE
    }

    private fun mostrarError(mensaje: String, permitirReintento: Boolean = true) {
        progreso.visibility = View.GONE
        contenido.visibility = View.GONE
        tvEstado.text = mensaje
        tvEstado.visibility = View.VISIBLE
        btnReintentar.visibility = if (permitirReintento) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        llamadaDetalle?.cancel()
        super.onDestroy()
    }
}
