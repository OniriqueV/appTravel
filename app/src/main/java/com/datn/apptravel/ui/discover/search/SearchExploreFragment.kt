//package com.datn.apptravel.ui.discover.search
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.EditText
//import android.widget.TextView
//import androidx.core.widget.doAfterTextChanged
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.datn.apptravel.R
//import com.datn.apptravel.ui.discover.adapter.DiscoverFeedAdapter
//import com.datn.apptravel.ui.discover.detail.PostDetailActivity
//import com.datn.apptravel.ui.discover.model.DiscoverItem
//import com.datn.apptravel.ui.discover.network.DiscoverRepository
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import org.koin.androidx.viewmodel.ext.android.viewModel
//import com.datn.apptravel.ui.discover.DiscoverViewModel
//
//class SearchExploreFragment : Fragment() {
//
//    private lateinit var etSearch: EditText
//    private lateinit var rvExplore: RecyclerView
//    private lateinit var rvCommunity: RecyclerView
//
//    private lateinit var exploreAdapter: ExploreCategoryAdapter
//    private lateinit var resultsAdapter: DiscoverFeedAdapter
//
//    private val viewModel: DiscoverViewModel by viewModel()
//    private val repo = DiscoverRepository()
//
//    private var searchJob: Job? = null
//    private val userId: String by lazy { arguments?.getString("userId").orEmpty() }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View = inflater.inflate(R.layout.fragment_search_explore, container, false)
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        etSearch = view.findViewById(R.id.etSearch)
//        rvExplore = view.findViewById(R.id.rvExplore)
//        rvCommunity = view.findViewById(R.id.rvCommunity)
//
//        setupExplore()
//        setupResults()
//
//        etSearch.doAfterTextChanged {
//            debounceSearch(it?.toString().orEmpty())
//        }
//
//        showExploreMode()
//    }
//
//    // =========================
//    // Explore grid
//    // =========================
//    private fun setupExplore() {
//        val categories = listOf(
//            "Cuisine", "Destination", "Adventure", "Resort",
//            "Camping", "Shopping", "Activity", "Tour"
//        )
//
//        exploreAdapter = ExploreCategoryAdapter(categories) { keyword ->
//            etSearch.setText(keyword)
//            etSearch.setSelection(keyword.length)
//            debounceSearch(keyword)
//        }
//
//        rvExplore.layoutManager = GridLayoutManager(requireContext(), 2)
//        rvExplore.adapter = exploreAdapter
//    }
//
//    // =========================
//    // Search result list
//    // =========================
//    private fun setupResults() {
//        resultsAdapter = DiscoverFeedAdapter(
//            onPostClick = { item ->
//                openPostDetail(item.postId)
//            },
//            onLike = { postId ->
//                viewModel.likePost(postId)
//            },
//            onComment = { postId ->
//                openPostDetail(postId)
//            }
//        )
//
//        rvCommunity.layoutManager = LinearLayoutManager(requireContext())
//        rvCommunity.adapter = resultsAdapter
//    }
//
//    private fun openPostDetail(postId: String?) {
//        if (postId.isNullOrBlank()) return
//        val i = Intent(requireContext(), PostDetailActivity::class.java)
//        i.putExtra(PostDetailActivity.EXTRA_POST_ID, postId)
//        i.putExtra("userId", userId)
//        startActivity(i)
//    }
//
//    // =========================
//    // Search logic
//    // =========================
//    private fun debounceSearch(raw: String) {
//        searchJob?.cancel()
//        searchJob = viewLifecycleOwner.lifecycleScope.launch {
//            delay(350)
//            val q = raw.trim()
//
//            if (q.isBlank()) {
//                resultsAdapter.submitList(emptyList())
//                showExploreMode()
//                return@launch
//            }
//
//            showResultMode()
//            try {
//                val data = repo.searchDiscover(q)
//                resultsAdapter.submitList(data)
//            } catch (_: Exception) {
//                resultsAdapter.submitList(emptyList())
//            }
//        }
//    }
//
//    private fun showExploreMode() {
//        rvExplore.visibility = View.VISIBLE
//        rvCommunity.visibility = View.GONE
//    }
//
//    private fun showResultMode() {
//        rvExplore.visibility = View.GONE
//        rvCommunity.visibility = View.VISIBLE
//    }
//}
//
///** Adapter explore category */
//private class ExploreCategoryAdapter(
//    private val items: List<String>,
//    private val onClick: (String) -> Unit
//) : RecyclerView.Adapter<ExploreCategoryAdapter.VH>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
//        val v = LayoutInflater.from(parent.context)
//            .inflate(android.R.layout.simple_list_item_1, parent, false)
//        return VH(v)
//    }
//
//    override fun getItemCount(): Int = items.size
//
//    override fun onBindViewHolder(holder: VH, position: Int) {
//        holder.bind(items[position], onClick)
//    }
//
//    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        fun bind(text: String, onClick: (String) -> Unit) {
//            (itemView as TextView).text = text
//            itemView.setOnClickListener { onClick(text) }
//        }
//    }
//}
