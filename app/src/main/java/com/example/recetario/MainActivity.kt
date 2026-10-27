package com.example.recetario

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo : EditText
    private lateinit var etPassword : EditText
    private lateinit var btnIniciar : Button

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btnRegistrarse = findViewById<Button>(R.id.btnRegistrarse)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnIniciar = findViewById(R.id.btnIniciar)

        dbHelper = DatabaseHelper(this)

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
