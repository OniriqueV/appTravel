package com.datn.apptravel.ui.discover.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.datn.apptravel.ui.discover.feed.RandomFeedFragment
import com.datn.apptravel.ui.discover.following.FollowingFragment

class DiscoverPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RandomFeedFragment()
            else -> FollowingFragment()
        }
    }
}
