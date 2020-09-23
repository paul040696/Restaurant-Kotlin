package com.example.fooddeliveryserver.Common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import com.example.fooddeliveryserver.Model.CategoryModel
import com.example.fooddeliveryserver.Model.FoodModel
import com.example.fooddeliveryserver.Model.ServerUserModel

object Common {

    val ORDER_REF: String = "Order"
    var foodSelected: FoodModel? = null
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
    var categorySelected: CategoryModel?=null
    const val CATEGORY_REF:String = "Category"
    const val SERVER_REF = "Server"
    var currentServerUser: ServerUserModel? = null

    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder, TextView.BufferType.SPANNABLE)

    }

    fun setSpanStringColor(welcome: String, name: String?, txtUser: TextView?, color: Int) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        txtSpannable.setSpan(ForegroundColorSpan(color),0,name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder, TextView.BufferType.SPANNABLE)

    }

    fun converStatusToString(orderStatus: Int): String? =
        when(orderStatus)
        {
            0 -> "Plasata"
            1 -> "Se livreaza"
            2 -> "Livrata"
            -1 -> "Anulata"
            else -> "Eroare"

        }

    enum class ACTION{
        CREATE,
        UPDATE,
        DELETE
    }
}