package com.datn.apptravel.ui.discover.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.feed.FollowingFragment
import com.datn.apptravel.ui.discover.feed.RandomFeedFragment

class DiscoverPagerAdapter(
    fragment: Fragment,
    private val userId: String
) : FragmentStateAdapter(fragment) {

    // ✅ Giữ instance fragment – KHÔNG tạo mới mỗi lần
    private val fragments: List<Fragment> = listOf(
        RandomFeedFragment(),
        FollowingFragment.newInstance(userId)
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * 🔁 Gọi khi Create Post xong
     * DiscoverFragment sẽ gọi hàm này
     */
    fun refresh() {
        fragments.forEach { fragment ->
            if (fragment is Refreshable) {
                fragment.onRefresh()
            }
        }
    }
}
