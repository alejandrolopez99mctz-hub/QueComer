package com.example.recetario

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
    private lateinit var etNombre : EditText
    private lateinit var etCorreo : EditText
    private lateinit var etPassword : EditText
    private lateinit var btnRegistro : Button

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnRegistro = findViewById(R.id.btnRegistro)

        dbHelper = DatabaseHelper(this)

        //CREAR EL EVENTO PARA REGISTRAR EL USUARIO
        btnRegistro.setOnClickListener {
            registrarUsuario()
        }


    }

    //METODO PARA REGISTRAR EL USUARIO
    private fun registrarUsuario(){
        //Obtener la informacion que el ususario escribe
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        //VALIDAR QUE EL USUARIO  LLENE LOS CAMPOS
        if(nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()){
            Toast.makeText(this,"Completa todos los campos",Toast.LENGTH_SHORT).show()
            return
        }

        val resultado = dbHelper.registrarUsuario(nombre, correo, pass)
        if(resultado){
            Toast.makeText(this,"Registro Exitoso",Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this,"Error al registrar",Toast.LENGTH_SHORT).show()
        }

    }
}