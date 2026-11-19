package com.example.recetario

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo : EditText
    private lateinit var etPassword : EditText
    private lateinit var btnIniciar : Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var btnGoogle: ImageButton
    private var conectandoGoogle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        val btnRegistrarse = findViewById<Button>(R.id.btnRegistrarse)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnIniciar = findViewById(R.id.btnIniciar)

        dbHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGoogle.setOnClickListener { iniciarSesionGoogle() }

        //CREAR EVENTO PARA INICIAR SESION
        btnIniciar.setOnClickListener {
            iniciarSesion()
        }

        //Crear evento para ir a otra pantalla
        btnRegistrarse.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) abrirInicio()
    }

    // El botón pide a Google una credencial y Firebase valida su ID token.
    private fun iniciarSesionGoogle() {
        if (conectandoGoogle) return
        mostrarCargaGoogle(true)
        val inicioGoogle = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id)
        ).build()
        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(inicioGoogle)
            .build()

        lifecycleScope.launch {
            try {
                val resultado = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = solicitud
                )
                procesarCredencial(resultado.credential)
            } catch (_: GetCredentialCancellationException) {
                mostrarCargaGoogle(false)
            } catch (_: GetCredentialException) {
                mostrarErrorGoogle(R.string.error_google)
            }
        }
    }

    private fun procesarCredencial(credential: Credential) {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            mostrarErrorGoogle(R.string.error_credencial_google)
            return
        }
        try {
            val credencialGoogle = GoogleIdTokenCredential.createFrom(credential.data)
            autenticarFirebase(credencialGoogle.idToken)
        } catch (_: GoogleIdTokenParsingException) {
            mostrarErrorGoogle(R.string.error_credencial_google)
        }
    }

    private fun autenticarFirebase(idToken: String) {
        val credencialFirebase = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencialFirebase).addOnCompleteListener(this) { inicio ->
            mostrarCargaGoogle(false)
            if (inicio.isSuccessful) {
                val usuario = auth.currentUser
                Toast.makeText(this, getString(R.string.bienvenida_google,
                    usuario?.displayName ?: usuario?.email.orEmpty()), Toast.LENGTH_SHORT).show()
                abrirInicio()
            } else {
                mostrarErrorGoogle(R.string.error_google)
            }
        }
    }

    private fun mostrarCargaGoogle(cargando: Boolean) {
        conectandoGoogle = cargando
        btnGoogle.isEnabled = !cargando
        btnIniciar.isEnabled = !cargando
        findViewById<Button>(R.id.btnRegistrarse).isEnabled = !cargando
    }

    private fun mostrarErrorGoogle(mensaje: Int) {
        mostrarCargaGoogle(false)
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun abrirInicio() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    //METODO PARA INICIAR SESION
    private fun iniciarSesion(){
        //Obtener la informacion que el usuario escribe
        val correo = etCorreo.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        //VALIDAR QUE EL USUARIO LLENE LOS CAMPOS
        if(correo.isEmpty() || pass.isEmpty()){
            Toast.makeText(this,"Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        //VALIDAR QUE EL CORREO TENGA UN FORMATO CORRECTO
        if(!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
            etCorreo.error = "Correo no valido"
            return
        }

        //CONSULTAR EN LA BASE DE DATOS SI EXISTE EL USUARIO
        val resultado = dbHelper.iniciarSesion(correo, pass)

        if(resultado){
            Toast.makeText(this,"Bienvenido", Toast.LENGTH_SHORT).show()

            //Ir a la pantalla principal
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)

            //Cerrar el login para no regresar con el boton atras
            finish()

        } else {
            Toast.makeText(this,"Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }

    }
}
