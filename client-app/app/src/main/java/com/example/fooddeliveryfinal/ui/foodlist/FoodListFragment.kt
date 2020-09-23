package com.example.fooddeliveryfinal.ui.foodlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fooddeliveryfinal.Adapter.MyFoodListAdapter
import com.example.fooddeliveryfinal.EventBus.MenuItemBack
import com.example.fooddeliveryfinal.R
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var foodListModel: FoodListModel

    var recycler_food_list : RecyclerView?=null
    var layoutAnimationController:LayoutAnimationController?=null

    var adapter: MyFoodListAdapter?=null

    override fun onStop() {

        if(adapter!=null)
            adapter!!.onStop()
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListModel =
            ViewModelProviders.of(this).get(FoodListModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)
        initViews(root);
        foodListModel.getMutableFoodModelListData().observe(this, Observer {
            adapter = MyFoodListAdapter(context!!,it)
            recycler_food_list!!.adapter = adapter
            recycler_food_list!!.layoutAnimation =layoutAnimationController
        })
        return root
    }

    private fun initViews(root: View?) {
        recycler_food_list = root!!.findViewById(R.id.recycler_food_list)
        recycler_food_list!!.setHasFixedSize(true)
        recycler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}