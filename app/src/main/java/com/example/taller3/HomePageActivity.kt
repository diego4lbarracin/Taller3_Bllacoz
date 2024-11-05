package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.InputStream

class HomePageActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var auth: FirebaseAuth
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userUid: String? = null  // UID del usuario autenticado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        userUid = intent.getStringExtra("user_uid")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val dotsIcon: ImageView = findViewById(R.id.dots_icon)
        dotsIcon.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_conectarse -> {
                        userUid?.let { updateUserStatus(it, true) }
                        Toast.makeText(this, "Conectado!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_desconectarse -> {
                        userUid?.let { updateUserStatus(it, false) }
                        Toast.makeText(this, "Desconectado!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_listaUsuarios -> {
                        val intent = Intent(this, ListadoUsuarios::class.java)
                        intent.putExtra("user_uid", userUid)  // Pasar UID al siguiente Activity
                        startActivity(intent)
                        true
                    }
                    R.id.action_logout -> {
                        auth.signOut()
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun updateUserStatus(userId: String, isConnected: Boolean) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("estado").setValue(isConnected)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadLocationsFromJson()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(currentLocation)
                            .title("Mi ubicación")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
    }

    private fun loadLocationsFromJson() {
        try {
            val inputStream: InputStream = assets.open("locations.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val locationsArray = jsonObject.getJSONArray("locationsArray")

            for (i in 0 until locationsArray.length()) {
                val locationObj = locationsArray.getJSONObject(i)
                val latitude = locationObj.getDouble("latitude")
                val longitude = locationObj.getDouble("longitude")
                val name = locationObj.getString("name")
                val position = LatLng(latitude, longitude)

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }
        } catch (e: Exception) {
            Log.e("HomePageActivity", "Error al leer locations.json", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(map)
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
