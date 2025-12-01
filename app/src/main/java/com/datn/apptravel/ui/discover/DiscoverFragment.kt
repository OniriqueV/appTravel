package com.datn.apptravel.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.datn.apptravel.R
import com.datn.apptravel.ui.activity.MainActivity
import com.datn.apptravel.ui.discover.adapter.DiscoverPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DiscoverFragment : Fragment() {

    private val viewModel: DiscoverViewModel by viewModels()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var layoutSearch: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabDiscover)
        viewPager = view.findViewById(R.id.vpDiscover)
        layoutSearch = view.findViewById(R.id.layoutSearch)

        // Setup ViewPager
        viewPager.adapter = DiscoverPagerAdapter(this)

        val tabTitles = arrayOf("Explore", "Following")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // 🔥 CLICK EVENT — chuyển sang trang tìm kiếm
        layoutSearch.setOnClickListener {
            (activity as MainActivity).openSearchExplore()

        }


    }
}
