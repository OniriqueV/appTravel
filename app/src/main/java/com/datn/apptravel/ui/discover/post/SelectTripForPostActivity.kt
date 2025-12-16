package com.datn.apptravel.ui.discover.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.TripItem
import com.datn.apptravel.ui.discover.network.TripApiClient
import kotlinx.coroutines.launch

class SelectTripForPostActivity : AppCompatActivity() {

    private lateinit var recyclerTrips: RecyclerView
    private lateinit var adapter: TripSelectAdapter

    private val list = mutableListOf<TripItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_trip_for_post)

        recyclerTrips = findViewById(R.id.recyclerTrips)

        adapter = TripSelectAdapter(list) { tripItem ->
            val data = Intent().apply {
                putExtra("tripId", tripItem.id)
                putExtra("tripTitle", tripItem.title)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        recyclerTrips.layoutManager = LinearLayoutManager(this)
        recyclerTrips.adapter = adapter

        val userId = intent.getStringExtra("userId").orEmpty()
        if (userId.isBlank()) {
            Toast.makeText(this, "Thiếu userId để load trips", Toast.LENGTH_SHORT).show()
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
                        val id = t.id ?: return@mapNotNull null   // ✅ fix lỗi String?
                        TripItem(
                            id = id,
                            title = t.title,                        // title là String (non-null)
                            coverPhoto = t.coverPhoto ?: ""
                        )
                    }

                    list.clear()
                    list.addAll(items)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        this@SelectTripForPostActivity,
                        "Load trips fail: ${res.code()}",
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
