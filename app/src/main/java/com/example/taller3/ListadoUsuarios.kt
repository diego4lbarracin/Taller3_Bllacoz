package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ListadoUsuarios : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var usuariosAdapter: UsuariosAdapter
    private val userList = mutableListOf<Usuario>()
    private var userUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listado_usuarios)

        userUid = intent.getStringExtra("user_uid")

        database = FirebaseDatabase.getInstance().getReference("users")

        val recyclerView = findViewById<RecyclerView>(R.id.lista)
        usuariosAdapter = UsuariosAdapter(userList) { usuario ->
            val intent = Intent(this, DistanciaUsuarios::class.java)
            intent.putExtra("nombre", usuario.name)
            intent.putExtra("latitud", usuario.latitud)
            intent.putExtra("longitud", usuario.longitud)
            startActivity(intent)
        }
        recyclerView.adapter = usuariosAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        monitorUserStatusChanges()
    }

    private fun monitorUserStatusChanges() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val usuario = snapshot.getValue(Usuario::class.java)
                usuario?.let {
                    if (it.estado) {
                        userList.add(it)
                        usuariosAdapter.notifyDataSetChanged()
                        Toast.makeText(this@ListadoUsuarios, "${it.name} está disponible", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val usuario = snapshot.getValue(Usuario::class.java)
                usuario?.let {
                    val index = userList.indexOfFirst { user -> user.name == it.name }
                    if (it.estado) {
                        if (index == -1) {
                            userList.add(it)
                            Toast.makeText(this@ListadoUsuarios, "${it.name} está disponible", Toast.LENGTH_SHORT).show()
                        } else {
                            userList[index] = it
                        }
                    } else {
                        if (index != -1) {
                            userList.removeAt(index)
                            Toast.makeText(this@ListadoUsuarios, "${it.name} se desconectó", Toast.LENGTH_SHORT).show()
                        }
                    }
                    usuariosAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java)
                usuario?.let {
                    userList.removeAll { user -> user.name == it.name }
                    usuariosAdapter.notifyDataSetChanged()
                    Toast.makeText(this@ListadoUsuarios, "${it.name} ha sido eliminado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListadoUsuarios, "Error al monitorear usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

