package com.datn.apptravel.ui.discover.adapter

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.feed.FollowingFragment
import com.datn.apptravel.ui.discover.feed.RandomFeedFragment

class DiscoverPagerAdapter(
    hostFragment: Fragment
) : FragmentStateAdapter(hostFragment) {

    private val fragments: List<Fragment> = listOf(
        RandomFeedFragment(),
        FollowingFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun refresh() {
        fragments.forEach { f ->
            val canRefresh =
                f is Refreshable &&
                        f.isAdded &&
                        f.view != null &&
                        f.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

            if (canRefresh) {
                (f as Refreshable).onRefresh()
            }
        }
    }
}
