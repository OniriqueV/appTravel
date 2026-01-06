package com.datn.apptravel.ui.discover.PlanMap.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.PlanMap.PlanMapDetailViewModel
import com.datn.apptravel.ui.discover.PlanMap.adapter.PlanMapCommentAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlanMapDetailCommentBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: PlanMapDetailViewModel by sharedViewModel()

    private lateinit var adapter: PlanMapCommentAdapter
    private lateinit var edtComment: EditText
    private lateinit var btnSend: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_plan_map_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val rv = view.findViewById<RecyclerView>(R.id.rvComments)
        edtComment = view.findViewById(R.id.edtComment)
        btnSend = view.findViewById(R.id.btnSend)

        adapter = PlanMapCommentAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.comments.observe(viewLifecycleOwner) {
            adapter.submit(it)
            rv.scrollToPosition(it.size - 1)
        }

        btnSend.setOnClickListener {
            val text = edtComment.text.toString()
            if (text.isNotBlank()) {
                viewModel.postComment(text)
                edtComment.setText("")
            }
        }
    }

    companion object {
        fun show(fm: FragmentManager) {
            PlanMapDetailCommentBottomSheet()
                .show(fm, "PlanMapDetailComment")
        }
    }
}