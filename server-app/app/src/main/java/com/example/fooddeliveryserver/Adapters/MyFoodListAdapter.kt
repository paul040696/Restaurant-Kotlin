package com.example.fooddeliveryserver.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fooddeliveryserver.Callback.IRecyclerItemClickListener
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.Model.FoodModel
import com.example.fooddeliveryserver.R
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class MyFoodListAdapter(
    internal var context: Context,
    internal var foodList: List<FoodModel>
) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList.get(position).image)
            .into(holder.img_food_image!!)
        holder.text_food_name!!.setText(foodList.get(position).name)
        holder.text_food_price!!.setText(StringBuilder(foodList.get(position).price.toString()).append(" LEI"))

        //Event
        holder.setListener(object : IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.foodSelected = foodList.get(pos)
                Common.foodSelected!!.key = pos.toString()
            }
        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_food_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    fun getItemAtPosition(pos: Int): FoodModel {
        return foodList.get(pos)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }


        var text_food_name: TextView? = null
        var text_food_price: TextView? = null

        var img_food_image: ImageView? = null
        var img_fav: ImageView? = null
        var img_cart: ImageView? = null

        internal var listener: IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }


        init {
            text_food_name = itemView.findViewById(R.id.text_food_name) as TextView
            text_food_price = itemView.findViewById(R.id.text_food_price) as TextView
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView

            itemView.setOnClickListener(this)
        }
    }

}