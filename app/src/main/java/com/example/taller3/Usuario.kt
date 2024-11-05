package com.example.taller3

data class Usuario(
    val name: String = "",
    val profileImageUrl: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    var estado: Boolean = false
)
