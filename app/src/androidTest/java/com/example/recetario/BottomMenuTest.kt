package com.example.recetario

import android.content.Intent
import android.graphics.Bitmap
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BottomMenuTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun favoritosAbrePantallaNegraVacia() {
        val monitor = instrumentation.addMonitor(FavoritesActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<HomeActivity>(Intent(instrumentation.targetContext, HomeActivity::class.java)).use { home ->
                home.onActivity { it.findViewById<BottomNavigationView>(R.id.barraInferior).selectedItemId = R.id.nav_favoritos }
                val favoritos = instrumentation.waitForMonitorWithTimeout(monitor, 10000)
                assertNotNull(favoritos)
                instrumentation.runOnMainSync {
                    val fondo = favoritos.findViewById<android.widget.FrameLayout>(R.id.favoritosVacio)
                    assertEquals(0, fondo.childCount)
                    assertEquals(android.graphics.Color.BLACK, (fondo.background as android.graphics.drawable.ColorDrawable).color)
                    favoritos.finish()
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun cerrarSesionAbreLoginYEliminaLaPila() {
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<HomeActivity>(Intent(instrumentation.targetContext, HomeActivity::class.java)).use { home ->
                home.onActivity { it.findViewById<BottomNavigationView>(R.id.barraInferior).selectedItemId = R.id.nav_salir }
                val login = instrumentation.waitForMonitorWithTimeout(monitor, 15000)
                assertNotNull("Debe regresar al login", login)
                assertNull(FirebaseAuth.getInstance().currentUser)
                assertTrue(login.intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
                assertTrue(login.intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
                instrumentation.runOnMainSync { login.finish() }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
