package com.example.fooddeliveryfinal.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.bumptech.glide.Glide
import com.example.fooddeliveryfinal.Callback.IRecyclerItemClickListener
import com.example.fooddeliveryfinal.EventBus.PopularFoodItemClick
import com.example.fooddeliveryfinal.Model.PopularCategoryModel
import com.example.fooddeliveryfinal.R
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.layout_popular_categories_item.view.*
import org.greenrobot.eventbus.EventBus
import java.security.AccessControlContext

class PopularCategoriesAdapter(
    internal var context: Context,
    internal var popularCategoryModels: List<PopularCategoryModel>
) :
    RecyclerView.Adapter<PopularCategoriesAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_popular_categories_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return popularCategoryModels.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(popularCategoryModels.get(position).image)
            .into(holder.category_image!!)
        holder.category_name!!.setText(popularCategoryModels.get(position).name)

        holder.setListener(object : IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {
                EventBus.getDefault()
                    .postSticky(PopularFoodItemClick(popularCategoryModels[pos]))
            }

        })
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!, adapterPosition)
        }

        var category_name: TextView? = null

        var category_image: CircleImageView? = null

        internal var listener: IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init {
            category_name = itemView.findViewById(R.id.text_category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as CircleImageView
            itemView.setOnClickListener(this)

        }
    }

}