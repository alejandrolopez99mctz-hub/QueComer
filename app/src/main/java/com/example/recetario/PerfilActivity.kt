package com.example.recetario

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

//PANTALLA DEL PERFIL DEL USUARIO
class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { usuario ->
            findViewById<TextView>(R.id.tvUsuario).apply {
                text = listOfNotNull(usuario.displayName, usuario.email).joinToString("\n")
                textSize = 18f
            }
        }

        //Crear evento para cerrar sesion y regresar al login
        btnCerrarSesion.setOnClickListener {
            btnCerrarSesion.isEnabled = false
            auth.signOut()
            lifecycleScope.launch {
                try {
                    CredentialManager.create(this@PerfilActivity)
                        .clearCredentialState(ClearCredentialStateRequest())
                } catch (_: ClearCredentialException) {
                    // Firebase ya cerró la sesión aunque el proveedor no pueda limpiarse.
                }
                val intent = Intent(this@PerfilActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                Toast.makeText(applicationContext, R.string.sesion_cerrada, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
