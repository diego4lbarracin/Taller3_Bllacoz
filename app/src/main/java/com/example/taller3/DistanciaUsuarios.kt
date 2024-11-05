package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class DistanciaUsuarios : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var userLat = 0.0
    private var userLng = 0.0
    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distancia_usuarios)

        userName = intent.getStringExtra("nombre") ?: "Usuario"
        userLat = intent.getDoubleExtra("latitud", 0.0)
        userLng = intent.getDoubleExtra("longitud", 0.0)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val userLocation = LatLng(userLat, userLng)
        map.addMarker(MarkerOptions().position(userLocation).title(userName))

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    map.addMarker(MarkerOptions().position(currentLocation).title("Mi ubicación"))

                    val results = FloatArray(1)
                    Location.distanceBetween(it.latitude, it.longitude, userLat, userLng, results)
                    val distanciaMetros = results[0]

                    Toast.makeText(this, "Distancia a $userName: ${distanciaMetros.toInt()} metros", Toast.LENGTH_LONG).show()

                    map.addPolyline(
                        PolylineOptions()
                            .add(currentLocation, userLocation)
                            .width(5f)  // Ancho de la línea
                            .color(ContextCompat.getColor(this, R.color.teal_700))
                    )
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
    }
}
