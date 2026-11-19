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
        private const val DATABASE_VERSION = 2
        private const val TABLE_USUARIOS = "usuarios"
        private const val TABLE_FAVORITOS = "favoritos"
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_CORREO = "correo"
        private const val COL_PASSWORD = "password"

        private const val COL_ID_FAVORITO = "id"
        private const val COL_ID_RECETA = "receta_id"
        private const val COL_NOMBRE_RECETA = "nombre"
        private const val COL_IMAGEN_RECETA = "receta"

        private const val CREATE_TABLE_FAVORITOS = """
            CREATE TABLE $TABLE_FAVORITOS (
            $COL_ID_FAVORITO INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ID_RECETA INTEGER UNIQUE NOT NULL,
            $COL_NOMBRE_RECETA TEXT,
            $COL_IMAGEN_RECETA TEXT
            )
        """

        private const val CREATE_TABLE_USUARIOS = """
            CREATE TABLE $TABLE_USUARIOS(
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CORREO TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL
            )
        """

    }

    //METODO PARA CREAR LA BASE DE DATOS Y SUS TABLAS
    override fun onCreate(db: SQLiteDatabase?) {

        db?.execSQL(CREATE_TABLE_USUARIOS)
        db?.execSQL(CREATE_TABLE_FAVORITOS)


    }

    //METODO PARA ACTUALIZAR LA BASE DE DATOS
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        if (oldVersion < 2){

            db?.execSQL(CREATE_TABLE_FAVORITOS)
        }
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

    fun guardarRecetaFavorita (idcomida : Long, nombre: String,  imagen : String) : Boolean{
        val db = writableDatabase

        val datos = ContentValues().apply {
            put(COL_ID_RECETA,idcomida)
            put(COL_NOMBRE_RECETA,nombre)
            put(COL_IMAGEN_RECETA,imagen)


        }
        val resultado = db.insertWithOnConflict(
            TABLE_FAVORITOS,
            null,
            datos,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        return resultado != -1L
    }

    fun eliminarRecetaFavorita (idcomida : Long) : Boolean{

        val db = writableDatabase
        val resultado = db.delete(
            TABLE_FAVORITOS,
            "$COL_ID_RECETA = ?",
            arrayOf(idcomida.toString())
        )
        return resultado > 0
    }

    fun existeRecetaFavorita (idcomida : Long ) : Boolean{

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_ID_FAVORITO FROM $TABLE_FAVORITOS WHERE $COL_ID_RECETA = ?",
            arrayOf(idcomida.toString())


        )
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe

    }




}