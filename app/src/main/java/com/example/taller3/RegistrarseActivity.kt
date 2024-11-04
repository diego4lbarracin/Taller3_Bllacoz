package com.example.taller3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import android.Manifest
import android.location.Location
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import java.io.File
import java.util.UUID

class RegistrarseActivity : AppCompatActivity(){
    /*Declarando variables para almacenar botones y demas informacion*/
    private lateinit var btnRegistrarUsuario: Button
    private lateinit var btnRegresar: LinearLayout
    private lateinit var inputNombre : EditText
    private lateinit var inputApellido : EditText
    private lateinit var inputEmail : EditText
    private lateinit var inputPassword : EditText
    private lateinit var inputNumeroIdentificacion : EditText
    private lateinit var btnTomarFoto : ImageView
    private lateinit var btnObtenerUbicacion : ImageView
    private var latitudObtenida: Double = 0.0
    private var longitudObtenida: Double = 0.0
    private val REQUEST_CODE_LOCATION_PERMISSION = 103
    var REQUEST_CODE_SUBIR_FOTO = 0
    private lateinit var imageUri : Uri

    /*Autenticacion*/
    private lateinit var auth: FirebaseAuth

    /*Base de Datos*/
    val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    val PATH_USERS="users/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_usuario)
        /*Obteniendo Botones*/
        btnRegistrarUsuario = findViewById(R.id.registrarse)
        btnRegresar = findViewById(R.id.regresar)
        btnTomarFoto = findViewById(R.id.tomarFoto)
        btnObtenerUbicacion = findViewById(R.id.obtenerUbi)
        /*Obteniendo Inputs*/
        inputNombre = findViewById(R.id.nombre)
        inputEmail = findViewById(R.id.email)
        inputApellido = findViewById(R.id.apellido)
        inputPassword = findViewById(R.id.password)
        inputNumeroIdentificacion = findViewById(R.id.numeroIdentificacion)
        auth = Firebase.auth
        /*Logica de los botones al presionarlos*/
        myRef = database.getReference(PATH_USERS)
        btnTomarFoto.setOnClickListener {
            val intent = Intent(this, SubirFotoActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SUBIR_FOTO) // Use a unique request code
        }
        btnObtenerUbicacion.setOnClickListener{
            // Check for location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permissions
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
            } else {
                // Permissions already granted, get location
                getLocation()
            }
        }
        btnRegresar.setOnClickListener {
            val intent = Intent(this, IniciarSesionActivity::class.java)
            startActivity(intent)
        }

        btnRegistrarUsuario.setOnClickListener {
            // Check if variables are valid
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (::imageUri.isInitialized && imageUri != null && latitudObtenida != 0.0 && longitudObtenida !=0.0) {
                // Variables are valid, proceed with registration
                crearUsuarioFirebaseAuth(email, password)
                val intent = Intent(this, IniciarSesionActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Usuario Registrado Correctamente.", Toast.LENGTH_SHORT).show()
            } else {
                // Variables are invalid, show a message or disable the button
                Toast.makeText(this, "Por favor, seleccione una imagen y haga click sobre la ubicación primero.", Toast.LENGTH_SHORT).show()// Or you can disable the button:
                btnRegistrarUsuario.isEnabled = false
            }
        }
    }
    // Function to get the device location
    @SuppressLint("MissingPermission") // Permission is checked before calling this function
    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitudObtenida = location.latitude
                    longitudObtenida = location.longitude
                    // Now you have the latitude and longitude
                    // ... (your logic to use the latitude and longitude) ...
                    Toast.makeText(this, "Ubicacion Obtenida Exitosamente!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "La ubicación obtenida está vacia.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception: Exception ->
                Toast.makeText(this, "Hubo un error.", Toast.LENGTH_SHORT).show()
            }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, get location
                getLocation()
            } else {// Location permission denied, handle accordingly
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun crearUsuarioFirebaseAuth(email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                    val user = auth.currentUser
                    if (user != null) {
                        val myUser = MyUser()
                        myUser.name = inputNombre.text.toString().trim()
                        myUser.apellido = inputApellido.text.toString().trim()
                        myUser.numeroIdentificacion = inputNumeroIdentificacion.text.toString().trim()
                        myUser.latitud = latitudObtenida
                        myUser.longitud = longitudObtenida
                        myUser.estado = false
                        if (::myRef.isInitialized) {
                            // myRef is initialized, you can use it
                            val key = myRef.push().key
                            myRef = database.getReference(PATH_USERS + key)
                            myRef.setValue(myUser)
                        } else {
                            // myRef is not initialized, handle accordingly
                        }
                    }
                } else {
                    Toast.makeText(this, "createUserWithEmail:Failure: " + task.exception.toString(),
                        Toast.LENGTH_SHORT).show()
                    task.exception?.message?.let { Log.e(TAG, it) }
                }
            }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SUBIR_FOTO && resultCode == Activity.RESULT_OK) {
            val imageUriString = data?.getStringExtra(SubirFotoActivity.EXTRA_IMAGE_URI)
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString) // Convert back to URI
                // Now you have the image URI in RegistrarseActivity
                Toast.makeText(this, "Imagen Obtenida Exitosamente!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun uploadFile(imageUri: Uri) { // Pass imageUri as a parameter
        val storageRef = FirebaseStorage.getInstance().getReference()// Get a reference to Firebase Storage
        // Create a unique filename for the image
        val imageFileName = "profile_images/${UUID.randomUUID()}.jpg"// Or any desired format
        val imageRef = storageRef.child(imageFileName)
        // Upload the image using the imageUri
        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Get a URL to the uploaded content
                Log.i("FBApp", "Successfully uploaded image")
                // You can get the download URL here if needed:
                // val downloadUri = taskSnapshot.metadata?.reference?.downloadUrl?.await()
            }
            .addOnFailureListener { exception ->
                // Handle unsuccessful uploads
                Log.e("FBApp", "Failed to upload image", exception)
            }
    }
    class MyUser{
        var name:String =""
        var apellido:String=""
        var numeroIdentificacion:String=""
        var latitud:Number=0.0
        var longitud:Number=0.0
        var estado:Boolean=false
    }

}