package com.example.fooddeliveryfinal.ui.home

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
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.asksira.loopingviewpager.LoopingViewPager
import com.example.fooddeliveryfinal.Adapter.MyBestDealsAdapter
import com.example.fooddeliveryfinal.Adapter.PopularCategoriesAdapter
import com.example.fooddeliveryfinal.R
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    @BindView(R.id.recycler_popular)

    var recyclerView:RecyclerView?=null
    var viewPager:LoopingViewPager?=null

    var layoutAnimationController:LayoutAnimationController?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)


        initView(root)

        //Bind DATA
        homeViewModel.popularList.observe(this, Observer {
            val listData = it
            val adapter = PopularCategoriesAdapter(context!!,listData)
            recyclerView!!.adapter = adapter
            recyclerView!!.layoutAnimation = layoutAnimationController
            })
        homeViewModel.bestDealList.observe(this, Observer {
            val adapter = MyBestDealsAdapter(context!!,it,false)
            viewPager!!.adapter = adapter
        })
        return root
    }

    private fun initView(root:View) {

    layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    viewPager = root.findViewById(R.id.viewpager) as LoopingViewPager
    recyclerView = root.findViewById(R.id.recycler_popular) as RecyclerView
    recyclerView!!.setHasFixedSize(true)
    recyclerView!!.layoutManager = LinearLayoutManager(context,RecyclerView.HORIZONTAL,false)
    }

    override fun onResume() {
        super.onResume()
        viewPager!!.resumeAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        viewPager!!.pauseAutoScroll()
    }
}