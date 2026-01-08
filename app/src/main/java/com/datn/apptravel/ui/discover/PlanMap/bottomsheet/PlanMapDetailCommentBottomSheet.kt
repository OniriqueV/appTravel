package com.datn.apptravels.ui.discover.PlanMap.bottomsheet

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
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.PlanMap.PlanMapDetailViewModel
import com.datn.apptravels.ui.discover.PlanMap.adapter.PlanMapCommentAdapter
import com.datn.apptravels.ui.discover.model.PlanCommentDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlanMapDetailCommentBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: PlanMapDetailViewModel by sharedViewModel()

    private lateinit var adapter: PlanMapCommentAdapter
    private lateinit var edtComment: EditText
    private lateinit var btnSend: TextView

    private var listener: CommentSheetListener? = null

    // 🔹 comment cha đang được reply
    private var replyParent: PlanCommentDto? = null

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
            onReplyClick = ::setReplyTo
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.isOwnerLive.observe(viewLifecycleOwner) { isOwner ->
            adapter.setOwner(isOwner)
        }

        viewModel.comments.observe(viewLifecycleOwner) {
            adapter.submit(it)
            if (it.isNotEmpty()) {
                rv.post { rv.scrollToPosition(it.size - 1) }
            }
        }

        // ✅ SEND COMMENT – CHUẨN 2 CẤP
        btnSend.setOnClickListener {
            val text = edtComment.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener

            viewModel.postComment(
                text = text,
                parentId = replyParent?.id?.toString()
            )

            // reset
            replyParent = null
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

    /**
     * display: comment được click (cha hoặc con)
     * parent: comment cha gốc
     */
    private fun setReplyTo(display: PlanCommentDto, parent: PlanCommentDto) {
        replyParent = parent
        adapter.expandParent(parent.id)
        edtComment.hint = "Reply"
        edtComment.requestFocus()
    }
}
