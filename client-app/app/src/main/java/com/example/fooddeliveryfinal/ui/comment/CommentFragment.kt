package com.example.fooddeliveryfinal.ui.comment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fooddeliveryfinal.Adapter.MyCommentAdapter
import com.example.fooddeliveryfinal.Callback.ICommentCallback
import com.example.fooddeliveryfinal.Common.Common
import com.example.fooddeliveryfinal.Model.CommentModel
import com.example.fooddeliveryfinal.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog

class CommentFragment : BottomSheetDialogFragment(), ICommentCallback {

    private var commentViewModel: CommentViewModel? = null

    private var recycler_comment: RecyclerView? = null

    private var listener: ICommentCallback

    private var dialog: AlertDialog? = null

    init {
        listener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_comment_fragment, container, false)
        initViews(itemView)
        loadCommentFromFirebase()
        commentViewModel!!.mutableLiveDataCommentList.observe(this, Observer { commentList ->
            val adapter = MyCommentAdapter(context!!, commentList)
            recycler_comment!!.adapter = adapter
        })
        return itemView
    }

    private fun loadCommentFromFirebase() {
        dialog!!.show()

        val commentModels = ArrayList<CommentModel>()
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
            .child(Common.foodSelected!!.id!!)
            .orderByChild("commentTimeStamp")
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onCommentLoadFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (commentSnapShot in p0.children) {
                        val commentModel = commentSnapShot.getValue<CommentModel>(CommentModel::class.java)
                        commentModels.add(commentModel!!)
                    }
                    listener.onCommentLoadSucces(commentModels)
                }
            })
    }

    private fun initViews(itemView: View?) {

        commentViewModel = ViewModelProviders.of(this).get(CommentViewModel::class.java)

        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        recycler_comment = itemView!!.findViewById(R.id.recycler_comment) as RecyclerView
        recycler_comment!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        recycler_comment!!.layoutManager = layoutManager
        recycler_comment!!.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))
    }

    override fun onCommentLoadSucces(commentList: List<CommentModel>) {
        dialog!!.dismiss()
        commentViewModel!!.setCommentList(commentList)
    }

    override fun onCommentLoadFailed(message: String) {
        Toast.makeText(context!!, "" + message, Toast.LENGTH_SHORT).show()
        dialog!!.dismiss()
    }

    companion object {
        private var instance: CommentFragment? = null

        fun getInstance(): CommentFragment {
            if (instance == null)
                instance = CommentFragment()
            return instance!!
        }
    }
}