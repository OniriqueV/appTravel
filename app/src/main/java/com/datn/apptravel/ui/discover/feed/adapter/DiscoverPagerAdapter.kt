package com.datn.apptravel.ui.discover.feed.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.feed.FollowingFragment
import com.datn.apptravel.ui.discover.feed.RandomFeedFragment

class DiscoverPagerAdapter(
    hostFragment: Fragment
) : FragmentStateAdapter(hostFragment) {

    // ✅ KHAI BÁO RÕ KIỂU Fragment
    private val fragments: List<Fragment> = listOf(
        RandomFeedFragment(),
        FollowingFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * 🔄 Refresh cả 2 tab (Explore + Following)
     */
    fun refresh() {
        fragments.forEach { fragment ->
            if (fragment is Refreshable) {
                fragment.onRefresh()
            }
        }
    }
}
