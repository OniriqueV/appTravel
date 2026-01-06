package com.datn.apptravels.ui.discover.post

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.model.TripItem
import com.datn.apptravels.ui.discover.network.TripApiClient
import com.datn.apptravels.ui.discover.post.adapter.TripSelectAdapter
import kotlinx.coroutines.launch

class SelectTripForPostActivity : AppCompatActivity() {

    private lateinit var recyclerTrips: RecyclerView
    private lateinit var adapter: TripSelectAdapter
    private val list = mutableListOf<TripItem>()

    companion object {
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_TRIP_TITLE = "tripTitle"
        const val EXTRA_TRIP_IMAGE = "tripImage"
        const val EXTRA_USER_ID = "userId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_trip_for_post)

        recyclerTrips = findViewById(R.id.recyclerTrips)

        adapter = TripSelectAdapter(list) { trip ->
            val data = Intent().apply {
                putExtra(EXTRA_TRIP_ID, trip.id)
                putExtra(EXTRA_TRIP_TITLE, trip.title ?: "")
                putExtra(EXTRA_TRIP_IMAGE, trip.coverPhoto ?: "")
            }
            setResult(RESULT_OK, data)
            finish()
        }

        recyclerTrips.layoutManager = LinearLayoutManager(this)
        recyclerTrips.adapter = adapter

        val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        if (userId.isBlank()) {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTrips(userId)
    }

    private fun loadTrips(userId: String) {
        lifecycleScope.launch {
            try {
                val res = TripApiClient.api.getTripsByUserId(userId)
                if (res.isSuccessful) {
                    val trips = res.body().orEmpty()

                    val items = trips.mapNotNull { t ->
                        val id = t.id ?: return@mapNotNull null
                        TripItem(
                            id = id,
                            title = t.title ?: "",
                            coverPhoto = t.coverPhoto ?: ""
                        )
                    }

                    list.clear()
                    list.addAll(items)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        this@SelectTripForPostActivity,
                        "Failed to load trips: ${res.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SelectTripForPostActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
