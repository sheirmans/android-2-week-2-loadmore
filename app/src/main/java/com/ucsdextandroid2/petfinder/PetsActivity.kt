package com.ucsdextandroid2.petfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import java.security.KeyStore

class PetsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val adapter = PetsAdapter()

    private val LOCATION_REQUEST_CODE = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pets)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
//        val adapter = PetsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        //LivePagedListBuilder of the PetsDataSourceFactory

        checkForLocationPermission(true)

        //showPermissionRationalDialog()
        //val didShowPermissionRational:Boolean = showPermissionRationalIfAble()
        //if(!didShowPermissionRational)
        //    onGetLocationFailed()

    }

    private fun checkForLocationPermission(showRational: Boolean){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
           // Toast.makeText(this,"Location Permission Granted",Toast.LENGTH_SHORT).show()
            getLocation()
        }
        else {
            //toast("Location Permission Denied")
           // if(showRational && ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
           if(!showRational || !showPermissionRationalIfAble()) {
               //showPermissionRational()
               ActivityCompat.requestPermissions(
                   this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                   LOCATION_REQUEST_CODE
               )
            }

          //  if(!showRational) {
          //      ActivityCompat.requestPermissions(his, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
          //  }
           // else {
           // ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
           // }
        }
    }

    private fun showPermissionRationalIfAble(): Boolean {
        val ableToShowRational = ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)
        if(ableToShowRational) {
            showPermissionRationalDialog()
            return true
        }
        else
            return false
    }

    private fun showPermissionRationalDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location")
            .setMessage("We need your location in order to show you pets in your area")
            .setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, which ->
                if(which == DialogInterface.BUTTON_POSITIVE)
                checkForLocationPermission(false)}
            )

            .setNegativeButton("No Thanks", DialogInterface.OnClickListener { dialog, which ->
                if(which == DialogInterface.BUTTON_NEGATIVE)
                    onGetLocationFailed()
            })
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        toast("Getting Location")

        LocationServices
            .getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location: Location ->

                val finalLat = location?.latitude ?: 40.toDouble()
                val finalLng = location?.longitude ?: (-73).toDouble()

             //   toast("Location Found ${location?.latitude}, ${location?.longitude}")
                setTitle("Finding Pet Near $finalLng,$finalLng")

                onLocationFound(finalLng, finalLng)

                if(location != null) {
                    onLocationFound(location.latitude,location.longitude)
                }
                else {
                    toast("Location was null")
                }
            }
            .addOnFailureListener {error ->
                toast(error.message ?: "Find Location Failed")
            }

/*        val client = LocationServices.getFusedLocationProviderClient(this)

        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locaionResul: LocationResult?) {
                super.onLocationResult(locationResult)

                val location = locationResult?.lastLocation

                toast("Location Found ${location?.latitude}, ${location?.longitude}")
                setTitle("${location?.latitude},${location?.longitude}")
            }
        }
        lifecycle.addObserver(object: DefaultLifecycleObserver {
            super.onResume(owner: LifecycleOwner)
            client.requestLocationUpdates(LocationRequest(), locationCallback, null)
        })


           override fun onPause(owner: LifecycleOwner){
               super.onResume()
           }
            .requestLocationUpdates(LocationRequest.create(),setMaxWait)*/
    }

    private fun onGetLocationFailed() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            toast("Getting Location Failed, go to settings to enable this")
        } else
            toast("Getting Locatio Failed")
        }

    private fun toast(toastMessage: String) {
        Toast.makeText(this,toastMessage,Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                           permissions: Array<out String>,
                                           grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)

        if(requestCode == LOCATION_REQUEST_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }
        else {
            onGetLocationFailed()
        }
    }

    private fun onLocationFound(lat: Double, lng:Double) {
        DataSource.findAnimals(lat, lng) { result ->
            toast("Found ${result.data?.animals?.size} Animals in your area")

        LivePagedListBuilder(PetsDataSourceFactory(lat,lng), 10)
            .build()
            .observe(this, Observer {
                adapter.submitList(it)
            })
        }
    }


    private class PetsAdapter: PagedListAdapter<PetModel,PetCardViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetCardViewHolder {
            return PetCardViewHolder.inflate(parent)
        }

        override fun onBindViewHolder(holder: PetCardViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        companion object {

            val diffCallback = object : DiffUtil.ItemCallback<PetModel>() {

                override fun areItemsTheSame(oldItem: PetModel, newItem: PetModel): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: PetModel, newItem: PetModel): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    private class PetCardViewHolder private constructor(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView = itemView.findViewById(R.id.vnc_image)
        val titleView: TextView = itemView.findViewById(R.id.vnc_title)
        val textView: TextView = itemView.findViewById(R.id.vnc_text)

        companion object {
            fun inflate(parent: ViewGroup): PetCardViewHolder = PetCardViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_note_card, parent, false)
            )
        }

        fun bind(model: PetModel?) {
            image.isVisible = model?.imageUrl != null
            image.loadImageUrl(model?.imageUrl)
            titleView.text = model?.name
            textView.text = "${model?.breed}\n ${model?.location}"
        }

    }
}
