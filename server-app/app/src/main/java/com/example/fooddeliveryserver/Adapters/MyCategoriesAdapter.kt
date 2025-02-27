package com.example.fooddeliveryserver.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fooddeliveryserver.Callback.IRecyclerItemClickListener
import com.example.fooddeliveryserver.EventBus.CategoryClick
import com.example.fooddeliveryserver.Model.CategoryModel
import com.example.fooddeliveryserver.R
import org.greenrobot.eventbus.EventBus

class MyCategoriesAdapter  (internal var context: Context,
                            internal var categoriesList: List<CategoryModel>):
    RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>(){
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(categoriesList.get(position).image)
            .into(holder.category_image!!)
        holder.category_name!!.setText(categoriesList.get(position).name)

        //Event

        //select category from list
        holder.setListener(object : IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                com.example.fooddeliveryserver.Common.Common.categorySelected = categoriesList.get(pos)
                EventBus.getDefault().postSticky(CategoryClick(true,categoriesList.get(pos)))
            }
        })


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCategoriesAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_category_item,
                parent,
                false
            )
        )    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(categoriesList.size == 1)
            com.example.fooddeliveryserver.Common.Common.DEFAULT_COLUMN_COUNT
        else {
            if (categoriesList.size % 2 == 0)
                com.example.fooddeliveryserver.Common.Common.DEFAULT_COLUMN_COUNT
            else
                if (position > 1 && position == categoriesList.size - 1) com.example.fooddeliveryserver.Common.Common.FULL_WIDTH_COLUMN else com.example.fooddeliveryserver.Common.Common.DEFAULT_COLUMN_COUNT
        }
    }

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }


        var category_name: TextView?=null

        var category_image: ImageView?=null

        internal var listener:IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init {
            category_name = itemView.findViewById(R.id.category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as ImageView
            itemView.setOnClickListener(this)
        }
    }

}