package com.example.taller3

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage

class UsuariosAdapter(
    private val userList: List<Usuario>,
    private val onLocationClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.pfp)
        val userName: TextView = view.findViewById(R.id.usuario)
        val locationButton: Button = view.findViewById(R.id.eliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_usuarios, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.name
        loadProfileImage(user.profileImageUrl, holder.profileImage)

        holder.locationButton.setOnClickListener {
            onLocationClick(user)
        }
    }


    override fun getItemCount() = userList.size

    private fun loadProfileImage(imageUrl: String, imageView: ImageView) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bmp)
        }.addOnFailureListener {
        }
    }
}

