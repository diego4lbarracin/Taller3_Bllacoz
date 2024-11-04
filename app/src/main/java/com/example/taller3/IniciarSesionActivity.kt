package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class IniciarSesionActivity : ComponentActivity() {
    /*Declaracion de la variable de auth*/
    private lateinit var auth: FirebaseAuth
    private lateinit var btnRegistrarUsuario: Button
    private lateinit var btnIniciarSesion: Button
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private val TAG = "SignInActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_sesion)
        /*Autenticacion*/
        auth = Firebase.auth
        /*Botones registrarse e iniciar sesion*/
        btnRegistrarUsuario = findViewById(R.id.registrarse)
        btnIniciarSesion = findViewById(R.id.login_button)
        /*Campos para iniciar sesion.*/
        emailField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)

        btnIniciarSesion.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            /*LLamada al servicio de autenticacion por email y password*/
            signInUser(email, password)
        }


        /*Cambiar a pantalla para registrar un usuario*/
        btnRegistrarUsuario.setOnClickListener{
            val intent = Intent(this, RegistrarseActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, HomePageActivity::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            emailField.setText("")
            passwordField.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = emailField.text.toString().trim()
        if (TextUtils.isEmpty(email)) {
            emailField.error = "Required."
            valid = false
        } else {
            emailField.error = null
        }
        val password = passwordField.text.toString().trim()
        if (TextUtils.isEmpty(password)) {
            passwordField.error = "Required."
            valid = false
        } else {
            passwordField.error = null
        }
        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5)
            return false
        return true
    }

    private fun signInUser(email: String, password: String){
        if(validateForm() && isEmailValid(email)){
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI
                        Log.d(TAG, "signInWithEmail:success:")
                        val user = auth.currentUser
                        updateUI(auth.currentUser)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }
}
