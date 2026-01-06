package com.datn.apptravel.ui.discover.PlanMap.bottomsheet

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.PlanMap.PlanMapDetailViewModel
import com.datn.apptravel.ui.discover.PlanMap.adapter.PlanMapCommentAdapter
import com.datn.apptravel.ui.discover.model.PlanCommentDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlanMapDetailCommentBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: PlanMapDetailViewModel by sharedViewModel()

    private lateinit var adapter: PlanMapCommentAdapter
    private lateinit var edtComment: EditText
    private lateinit var btnSend: TextView

    private var listener: CommentSheetListener? = null
    private var replyToComment: PlanCommentDto? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CommentSheetListener) {
            listener = context
        }
    }

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

        adapter = PlanMapCommentAdapter(
            currentUserId = viewModel.currentUserId().orEmpty(),
            onLongClick = ::showDeleteDialog,
            onReplyClick = ::setReplyTo // 🔥
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // ✅ QUAN TRỌNG: observe owner TRƯỚC
        viewModel.isOwnerLive.observe(viewLifecycleOwner) { isOwner ->
            adapter.setOwner(isOwner)
        }

        viewModel.comments.observe(viewLifecycleOwner) {
            adapter.submit(it)
            if (it.isNotEmpty()) {
                rv.post { rv.scrollToPosition(it.size - 1) }
            }
        }

        btnSend.setOnClickListener {
            val text = edtComment.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener

            if (replyToComment != null) {
                viewModel.postComment(
                    text,
                    replyToComment!!.id.toString()
                )
            } else {
                viewModel.postComment(text)
            }

            replyToComment = null
            edtComment.setText("")
            edtComment.hint = "Add a comment..."
        }

    }


    override fun onStart() {
        super.onStart()
        listener?.onCommentSheetShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onCommentSheetDismissed()
    }

    private fun showDeleteDialog(comment: PlanCommentDto) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete comment?")
            .setMessage("Bạn có chắc muốn xoá comment này không?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteComment(comment.id)
            }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.parseColor("#D32F2F"))

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.parseColor("#555555"))
    }

    companion object {
        fun show(fm: FragmentManager) {
            PlanMapDetailCommentBottomSheet()
                .show(fm, "PlanMapDetailComment")
        }
    }

    interface CommentSheetListener {
        fun onCommentSheetShown()
        fun onCommentSheetDismissed()
    }

    private fun setReplyTo(comment: PlanCommentDto) {
        replyToComment = comment
        edtComment.hint = "Reply to ${comment.userName ?: "user"}"
        edtComment.requestFocus()
    }

}
