package com.example.taller3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.location.Location
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class RegistrarseActivity : AppCompatActivity() {
    // Declarando variables de UI y Firebase
    private lateinit var btnRegistrarUsuario: Button
    private lateinit var btnRegresar: LinearLayout
    private lateinit var inputNombre: EditText
    private lateinit var inputApellido: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputNumeroIdentificacion: EditText
    private lateinit var btnTomarFoto: ImageView
    private lateinit var btnObtenerUbicacion: ImageView
    private var latitudObtenida: Double = 0.0
    private var longitudObtenida: Double = 0.0
    private val REQUEST_CODE_LOCATION_PERMISSION = 103
    var REQUEST_CODE_SUBIR_FOTO = 0
    private lateinit var imageUri: Uri

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private val PATH_USERS = "users/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_usuario)

        // Inicialización de elementos UI
        btnRegistrarUsuario = findViewById(R.id.registrarse)
        btnRegresar = findViewById(R.id.regresar)
        btnTomarFoto = findViewById(R.id.tomarFoto)
        btnObtenerUbicacion = findViewById(R.id.obtenerUbi)
        inputNombre = findViewById(R.id.nombre)
        inputEmail = findViewById(R.id.email)
        inputApellido = findViewById(R.id.apellido)
        inputPassword = findViewById(R.id.password)
        inputNumeroIdentificacion = findViewById(R.id.numeroIdentificacion)

        auth = FirebaseAuth.getInstance()
        myRef = database.getReference(PATH_USERS)

        btnRegistrarUsuario.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (::imageUri.isInitialized && latitudObtenida != 0.0 && longitudObtenida != 0.0) {
                // Autentica de forma anónima y luego sube la imagen
                authenticateAnonymouslyAndUploadImage(email, password)
            } else {
                Toast.makeText(this, "Por favor, seleccione una imagen y haga click sobre la ubicación primero.", Toast.LENGTH_SHORT).show()
            }
        }

        btnObtenerUbicacion.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
            } else {
                getLocation()
            }
        }

        btnTomarFoto.setOnClickListener {
            val intent = Intent(this, SubirFotoActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SUBIR_FOTO)
        }

        btnRegresar.setOnClickListener {
            val intent = Intent(this, IniciarSesionActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitudObtenida = location.latitude
                    longitudObtenida = location.longitude
                    Toast.makeText(this, "Ubicación obtenida con éxito!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "La ubicación obtenida está vacía.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener la ubicación.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun authenticateAnonymouslyAndUploadImage(email: String, password: String) {
        // Autentica al usuario de forma anónima
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Usuario autenticado anónimamente, ahora sube la imagen
                    uploadProfileImageAndRegisterUser(email, password)
                } else {
                    Toast.makeText(this, "Error en la autenticación anónima.", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error en autenticación anónima: ${task.exception}")
                }
            }
    }

    private fun uploadProfileImageAndRegisterUser(email: String, password: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageFileName = "profile_images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(imageFileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Imagen subida exitosamente.")
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d(TAG, "URL de la imagen: $downloadUrl")
                    // Ahora registra al usuario con email y password, y vincula la cuenta
                    registerAndLinkUserWithImageUrl(email, password, downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al subir imagen.", it)
                Toast.makeText(this, "Error al subir la imagen a Firebase Storage.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registerAndLinkUserWithImageUrl(email: String, password: String, profileImageUrl: String) {
        val authCredential = EmailAuthProvider.getCredential(email, password)
        auth.currentUser?.linkWithCredential(authCredential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Cuenta vinculada exitosamente.")
                    saveUserDataToDatabase(email, profileImageUrl)
                } else {
                    Log.e(TAG, "Error al vincular la cuenta.", task.exception)
                    Toast.makeText(this, "Error al vincular la cuenta.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserDataToDatabase(email: String, profileImageUrl: String) {
        val myUser = MyUser(
            name = inputNombre.text.toString().trim(),
            apellido = inputApellido.text.toString().trim(),
            numeroIdentificacion = inputNumeroIdentificacion.text.toString().trim(),
            latitud = latitudObtenida,
            longitud = longitudObtenida,
            estado = false,
            profileImageUrl = profileImageUrl
        )
        val key = myRef.push().key
        if (key != null) {
            myRef.child(key).setValue(myUser).addOnCompleteListener { saveTask ->
                if (saveTask.isSuccessful) {
                    Log.d(TAG, "Datos del usuario guardados en Firebase Database.")
                    Toast.makeText(this, "Usuario registrado correctamente.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, IniciarSesionActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "Error al guardar datos del usuario.", saveTask.exception)
                    Toast.makeText(this, "Error al guardar datos en Firebase Database.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SUBIR_FOTO && resultCode == Activity.RESULT_OK) {
            val imageUriString = data?.getStringExtra(SubirFotoActivity.EXTRA_IMAGE_URI)
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString)
                Toast.makeText(this, "Imagen obtenida exitosamente!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class MyUser(
        var name: String = "",
        var apellido: String = "",
        var numeroIdentificacion: String = "",
        var latitud: Double = 0.0,
        var longitud: Double = 0.0,
        var estado: Boolean = false,
        var profileImageUrl: String = ""
    )
}
