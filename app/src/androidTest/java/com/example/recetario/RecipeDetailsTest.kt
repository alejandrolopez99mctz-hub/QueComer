package com.example.recetario

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recetario.network.MealApi
import com.example.recetario.network.RetrofitClient
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Comprueba el flujo real de TheMealDB y captura evidencias; requiere conexión. */
@RunWith(AndroidJUnit4::class)
class RecipeDetailsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun configurarCertificadoOpcionalDelEntorno() {
        // Sólo para emuladores detrás de un antivirus/proxy HTTPS local.
        // El certificado debe proporcionarse explícitamente como argumento de prueba.
        // No desactiva la validación TLS ni cambia el cliente de la app distribuida.
        val ruta = InstrumentationRegistry.getArguments().getString("testCa") ?: return
        val certificado = File(ruta).inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        val almacen = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("entorno-pruebas", certificado)
        }
        val gestores = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(almacen)
        }
        val gestor = gestores.trustManagers.filterIsInstance<X509TrustManager>().single()
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(gestor), null) }
        HttpsURLConnection.setDefaultSSLSocketFactory(ssl.socketFactory)
        val cliente = OkHttpClient.Builder().sslSocketFactory(ssl.socketFactory, gestor).build()
        val api = Retrofit.Builder().baseUrl("https://www.themealdb.com/api/json/v1/1/")
            .client(cliente).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MealApi::class.java)
        // Sustituye el cliente sólo en este proceso de instrumentación.
        val delegado = RetrofitClient::class.java.getDeclaredField("api\$delegate").apply {
            isAccessible = true
        }.get(RetrofitClient)
        delegado.javaClass.getDeclaredField("_value").apply {
            isAccessible = true
            set(delegado, api)
        }
    }

    private fun esperar(condicion: () -> Boolean) {
        val limite = SystemClock.elapsedRealtime() + 45000
        while (SystemClock.elapsedRealtime() < limite) {
            var listo = false
            instrumentation.runOnMainSync { listo = condicion() }
            if (listo) return
            SystemClock.sleep(200)
        }
        fail("La API o la pantalla no estuvo disponible en 45 segundos")
    }

    private fun captura(nombre: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(1200)
        val salidaPrueba = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val destino = if (salidaPrueba != null) File(salidaPrueba, "evidencias")
            else File(instrumentation.targetContext.getExternalFilesDir(null), "evidencias")
        destino.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap)
        File(destino, "$nombre.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun tarjetaAbreDetalleYFavoritoEsTemporal() {
        val intent = Intent(instrumentation.targetContext, HomeActivity::class.java)
        ActivityScenario.launch<HomeActivity>(intent).use { home ->
            lateinit var actividad: HomeActivity
            home.onActivity { actividad = it }
            esperar { actividad.findViewById<LinearLayout>(R.id.contenedorRecetas).childCount > 0 }
            captura("01_lista_recetas")
            var nombre = ""
            instrumentation.runOnMainSync {
                val tarjeta = actividad.findViewById<LinearLayout>(R.id.contenedorRecetas).getChildAt(0)
                nombre = tarjeta.findViewById<TextView>(R.id.tvNombreReceta).text.toString()
            }
            val monitor = instrumentation.addMonitor(DetailsActivity::class.java.name, null, false)
            lateinit var detalle: DetailsActivity
            try {
                instrumentation.runOnMainSync {
                    actividad.findViewById<LinearLayout>(R.id.contenedorRecetas).getChildAt(0).performClick()
                }
                detalle = instrumentation.waitForMonitorWithTimeout(monitor, 10000) as DetailsActivity
            } finally {
                instrumentation.removeMonitor(monitor)
            }
            esperar { detalle.findViewById<View>(R.id.contenidoDetalle).visibility == View.VISIBLE }
            instrumentation.runOnMainSync {
                assertEquals(nombre, detalle.intent.getStringExtra(DetailsActivity.EXTRA_NOMBRE_RECETA))
                assertEquals(nombre, detalle.findViewById<TextView>(R.id.tvNombreDetalle).text.toString())
                assertTrue(detalle.findViewById<TextView>(R.id.tvIngredientesDetalle).text.isNotBlank())
                assertFalse(detalle.findViewById<ImageButton>(R.id.btnFavorito).isSelected)
            }
            captura("02_detalle_receta")
            instrumentation.runOnMainSync {
                detalle.findViewById<ImageButton>(R.id.btnFavorito).performClick()
                assertTrue(detalle.findViewById<ImageButton>(R.id.btnFavorito).isSelected)
                assertEquals(detalle.getString(R.string.quitar_favorito),
                    detalle.findViewById<ImageButton>(R.id.btnFavorito).contentDescription)
            }
            SystemClock.sleep(2500)
            captura("03_receta_favorita")
            instrumentation.runOnMainSync {
                detalle.findViewById<ImageButton>(R.id.btnFavorito).performClick()
                assertFalse(detalle.findViewById<ImageButton>(R.id.btnFavorito).isSelected)
            }
            SystemClock.sleep(2500)
            captura("04_favorito_desactivado")
            instrumentation.runOnMainSync { detalle.findViewById<View>(R.id.btnVolverRecetas).performClick() }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertTrue(actividad.findViewById<View>(R.id.btnMexico).isSelected)
                assertTrue(actividad.findViewById<LinearLayout>(R.id.contenedorRecetas).childCount > 0)
            }
        }
    }
}
