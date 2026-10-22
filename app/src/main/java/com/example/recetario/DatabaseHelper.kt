package com.example.recetario

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context : Context) : SQLiteOpenHelper(

    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION

) {

    companion object{
        private const val DATABASE_NAME = "recetario.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USUARIOS = "usuarios"
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_CORREO = "correo"
        private const val COL_PASSWORD = "password"
    }

    //METODO PARA CREAR LA BASE DE DATOS Y SUS TABLAS
    override fun onCreate(db: SQLiteDatabase?) {
        val crearTablaUsuarios = """
            CREATE TABLE $TABLE_USUARIOS(
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CORREO TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        db?.execSQL(crearTablaUsuarios)

    }

    //METODO PARA ACTUALIZAR LA BASE DE DATOS
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }

    //metodo para registrar un usuario
    fun registrarUsuario(nombre : String, correo : String, password : String) : Boolean{
        //CREAR VARIABLE CON PERMISOS DE ESCRIBIR EN LA DB
        val db = writableDatabase

        //CREAR VARIABLE PARA GUARDAR LOS DATOS
        val datos = ContentValues().apply{
            put(COL_NOMBRE, nombre)
            put(COL_CORREO, correo)
            put(COL_PASSWORD, password)
        }

        val resultado = db.insert(TABLE_USUARIOS, null, datos)

        return resultado != -1L


    }

    //METODO PARA LA CONSULTA DE USUARIOS (INICIO DE SESION)
    fun iniciarSesion(correo: String, password: String) : Boolean{
        //CREAR VARIABLE CON PERMISOS DE LEER LA DB
        val db = readableDatabase

        //CONSULTA QUE BUSCA UN USUARIO CON ESE CORREO Y CONTRASEÑA
        val query_sql = """
            SELECT * FROM $TABLE_USUARIOS
            WHERE $COL_CORREO = ? AND $COL_PASSWORD = ?
            """.trimIndent()

        val cursor = db.rawQuery(
            query_sql, arrayOf(correo, password)
        )

        //Contar el numero de resultados
        val usuarioEncontrado = cursor.count > 0

        cursor.close()
        db.close()

        return usuarioEncontrado

    }

}