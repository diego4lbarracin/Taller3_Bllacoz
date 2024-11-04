package com.example.taller3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SubirFotoActivity :AppCompatActivity() {
    private lateinit var circleImageView: CircleImageView
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_GALLERY = 1
        private const val REQUEST_CODE_CAMERA = 2
        private const val REQUEST_PERMISSION_READ_STORAGE = 100
        private const val REQUEST_PERMISSION_CAMERA = 101
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subir_foto)

        circleImageView = findViewById(R.id.pfp)
        val subirFotoButton: Button = findViewById(R.id.subirFoto)
        val tomarFotoButton: Button = findViewById(R.id.tomarFoto)
        val regresarButton: LinearLayout = findViewById(R.id.regresar)

        cargarFotoPerfil()

        subirFotoButton.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_PERMISSION_READ_STORAGE
                    )
                } else {
                    openGallery()
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_PERMISSION_READ_STORAGE
                    )
                } else {
                    openGallery()
                }
            }
        }


        tomarFotoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION_CAMERA)
            } else {
                openCamera()
            }
        }

        regresarButton.setOnClickListener {
            guardarFotoPerfil()
            val intent = Intent(this, RegistrarseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CODE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let {
                        val localUri = copyFileToInternalStorage(it)
                        if (localUri != null) {
                            selectedImageUri = localUri
                            circleImageView.setImageURI(localUri)
                            // Return the URI
                            val resultIntent = Intent()
                            resultIntent.putExtra(EXTRA_IMAGE_URI, selectedImageUri.toString())
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                    photo?.let {
                        circleImageView.setImageBitmap(it)
                        selectedImageUri = saveImageToInternalStorage(it)
                        // Return the URI
                        val resultIntent = Intent()
                        resultIntent.putExtra(EXTRA_IMAGE_URI, selectedImageUri.toString())
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }
    }

    private fun guardarFotoPerfil() {
        val sharedPref = getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fotoPerfil", selectedImageUri?.path ?: "")
            apply()
        }
    }

    private fun cargarFotoPerfil() {
        val sharedPref = getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE)
        val fotoPerfilPath = sharedPref.getString("fotoPerfil", "")
        if (!fotoPerfilPath.isNullOrEmpty()) {
            val file = File(fotoPerfilPath)
            if (file.exists()) {
                circleImageView.setImageURI(Uri.fromFile(file))
            } else {
                circleImageView.setImageResource(R.drawable.logo_capybara)
            }
        } else {
            circleImageView.setImageResource(R.drawable.logo_capybara)
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri? {
        val filename = "profile_pic_${System.currentTimeMillis()}.png"
        val file = File(filesDir, filename)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return Uri.fromFile(file)
    }

    private fun copyFileToInternalStorage(uri: Uri): Uri? {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val filename = "profile_pic_${System.currentTimeMillis()}.png"
            val file = File(filesDir, filename)
            FileOutputStream(file).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}