package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.components.Component

class HomePageActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        val dotsIcon : ImageView = findViewById(R.id.dots_icon)
        auth = Firebase.auth
        dotsIcon.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_options, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.action_conectarse -> {
//                        val intent = Intent(this, CrearParche::class.java)
//                        startActivity(intent)
                        Toast.makeText(this, "Conectado!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_desconectarse -> {
//                        val intent = Intent(this, MisParches::class.java)
//                        startActivity(intent)
                        Toast.makeText(this, "Desconectado!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_logout -> {
                        val intent = Intent(this, IniciarSesionActivity::class.java)
                        auth.signOut()
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}