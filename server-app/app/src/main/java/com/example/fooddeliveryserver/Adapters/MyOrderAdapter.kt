package com.example.fooddeliveryserver.Adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TabHost
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fooddeliveryserver.Callback.IRecyclerItemClickListener
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.Model.CartItem
import com.example.fooddeliveryserver.Model.FoodModel
import com.example.fooddeliveryserver.Model.OrderModel
import com.example.fooddeliveryserver.R
import com.example.fooddeliveryserver.ui.Order.OrderViewModel
import kotlinx.android.synthetic.main.layout_food_item.view.*
import java.text.SimpleDateFormat

class MyOrderAdapter(
    internal var context: Context,
    internal var orderList: MutableList<OrderModel>
) :
    RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    lateinit var simpleDateFormat: SimpleDateFormat

    init {
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    class MyViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {


        var text_time: TextView? = null
        var text_order_number: TextView? = null
        var text_order_status: TextView? = null
        var text_num_item: TextView? = null
        var text_name: TextView? = null

        var img_food_image: ImageView? = null

        internal var iRecyclerItemClickListener: IRecyclerItemClickListener?=null

        fun setListener(iRecyclerItemClickListener: IRecyclerItemClickListener)
        {
            this.iRecyclerItemClickListener = iRecyclerItemClickListener
        }

        init {
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView

            text_time = itemView.findViewById(R.id.txt_time) as TextView
            text_order_number = itemView.findViewById(R.id.txt_order_numer) as TextView
            text_order_status = itemView.findViewById(R.id.txt_order_status) as TextView
            text_num_item = itemView.findViewById(R.id.txt_num_item) as TextView
            text_name = itemView.findViewById(R.id.txt_name) as TextView

            itemView.setOnClickListener(this)

        }

        override fun onClick(p0: View?) {
            iRecyclerItemClickListener!!.onItemClick(p0!!,adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.layout_order_item,parent,false))
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(orderList[position].cartItemList!![0].foodImage)
            .into(holder.img_food_image!!)
        holder.text_order_number!!. setText(orderList[position].key)

        Common.setSpanStringColor("Data comenzii",simpleDateFormat.format(orderList[position].createDate),
            holder.text_time, Color.parseColor("#333639"))

        Common.setSpanStringColor("Starea comenzii",Common.converStatusToString(orderList[position].orderStatus),
            holder.text_order_status, Color.parseColor("#005758"))

        Common.setSpanStringColor("Numar produse", if(orderList[position].cartItemList == null) "0"
            else orderList[position].cartItemList!!.size.toString(),
            holder.text_num_item, Color.parseColor("#00574B"))

        Common.setSpanStringColor("Nume",orderList[position].userName,
            holder.text_name, Color.parseColor("#006061"))


        holder.setListener(object :IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {
                showDialog(orderList[pos].cartItemList)
            }

        })
    }

    private fun showDialog(cartItemList: List<CartItem>?) {
        val layout_dialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_detail,null)
        val builder = AlertDialog.Builder(context)
        builder.setView(layout_dialog)

        val btn_ok = layout_dialog.findViewById<View>(R.id.btn_ok) as Button
        val recycler_order_detail = layout_dialog.findViewById<View>(R.id.recycler_order_detail) as RecyclerView
        recycler_order_detail.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recycler_order_detail.layoutManager = layoutManager
        recycler_order_detail.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
        val adapter = MyOrderDetailAdapter(context,cartItemList!!.toMutableList())
        recycler_order_detail.adapter = adapter

        val dialog = builder.create()
        dialog.show()

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        btn_ok.setOnClickListener{dialog.dismiss()}

    }

    fun getItemAtPosition(pos: Int): OrderModel {
        return orderList[pos]

    }

    fun removeItem(pos: Int) {
        orderList.removeAt(pos)
    }


}