package com.datn.apptravel.ui.discover.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.adapter.CommunityAdapter
import com.datn.apptravel.ui.discover.adapter.ExploreAdapter
import com.datn.apptravel.ui.discover.model.CommunityItem
import com.datn.apptravel.ui.discover.model.ExploreItem

class SearchExploreFragment : Fragment() {

    private lateinit var rvExplore: RecyclerView
    private lateinit var rvCommunity: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvExplore = view.findViewById(R.id.rvExplore)
        rvCommunity = view.findViewById(R.id.rvCommunity)

        setupExplore()
        setupCommunity()
    }

    private fun setupExplore() {
        val items = listOf(
            ExploreItem("Beach", R.drawable.beach),
            ExploreItem("Mountain", R.drawable.mountain),
            ExploreItem("Forest", R.drawable.forest),
            ExploreItem("City", R.drawable.city)
        )

        rvExplore.layoutManager = GridLayoutManager(requireContext(), 2)
        rvExplore.adapter = ExploreAdapter(items)
    }

    private fun setupCommunity() {

        val community = listOf(
            CommunityItem(
                title = "Trip to Paris",
                imageRes = R.drawable.beach, // đổi sau theo đúng ảnh
                userName = "Anna",
                userAvatarRes = R.drawable.ic_avatar_placeholder
            ),
            CommunityItem(
                title = "Da Nang",
                imageRes = R.drawable.forest,  // đổi sau
                userName = "John",
                userAvatarRes = R.drawable.ic_avatar_placeholder
            )
        )

        rvCommunity.layoutManager =
            GridLayoutManager(requireContext(), 2)

        rvCommunity.adapter = CommunityAdapter(items = community)
    }
}
