package com.example.fooddeliveryfinal.Callback

import com.example.fooddeliveryfinal.Model.CommentModel

interface ICommentCallback {
    fun onCommentLoadSucces(commentList:List<CommentModel>)
    fun onCommentLoadFailed(message:String)
}
