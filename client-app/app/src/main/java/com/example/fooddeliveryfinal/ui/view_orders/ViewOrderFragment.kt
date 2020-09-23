package com.example.fooddeliveryfinal.ui.view_orders

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.Unbinder
import com.example.fooddeliveryfinal.Adapter.MyOrderAdapter
import com.example.fooddeliveryfinal.Callback.ILoadOrderCallbackListener
import com.example.fooddeliveryfinal.Callback.IMyButtonCallback
import com.example.fooddeliveryfinal.Common.Common
import com.example.fooddeliveryfinal.Common.MySwipeHelper
import com.example.fooddeliveryfinal.Database.CartDataSource
import com.example.fooddeliveryfinal.Database.CartDatabase
import com.example.fooddeliveryfinal.Database.LocalCartDataSource
import com.example.fooddeliveryfinal.EventBus.MenuItemBack
import com.example.fooddeliveryfinal.Model.Order
import com.example.fooddeliveryfinal.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class ViewOrderFragment :Fragment(), ILoadOrderCallbackListener
{
    private var viewOrderModel : ViewOrderModel?=null

    lateinit var cartDataSource: CartDataSource
    var compositeDisposable = CompositeDisposable()

    internal lateinit var recycler_order:RecyclerView

    internal lateinit var dialog: AlertDialog

    internal lateinit var listener:ILoadOrderCallbackListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewOrderModel = ViewModelProviders.of(this).get(ViewOrderModel::class.java!!)
        val root = inflater.inflate(R.layout.fragment_view_orders,container,false)
        initViews(root)
        loadOrderFromFirebase()

        viewOrderModel!!.mutableLiveDataOrderList.observe(this, Observer {
            Collections.reverse(it!!)
            val adapter = MyOrderAdapter(context!!,it!!)
            recycler_order!!.adapter = adapter
        } )

        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<Order>()

        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userId")
            .equalTo(Common.currentUser!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for(orderSnapShot in p0.children)
                    {
                        val order = orderSnapShot.getValue(Order::class.java)
                        order!!.orderNumber = orderSnapShot.key
                        orderList.add(order!!)
                    }
                    listener.onLoadOrderSuccess(orderList)
                }
            })
    }

    private fun initViews(root: View?) {

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        listener = this

        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context!!)
        recycler_order.layoutManager = layoutManager
        recycler_order.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))


        /*val swipe = object : MySwipeHelper(context!!,recycler_order!!,250){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {

                //Repeta comanda
                buffer.add(
                    MyButton(context!!,
                        "Repetă comanda",
                        30,
                        0,
                        Color.parseColor("FF3C36"),
                        object : IMyButtonCallback{
                            override fun onClick(pos: Int) {

                                val orderModel = (recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)
                                dialog.show()
                                //clear previous items from cart
                                cartDataSource.cleanCart(Common.currentUser!!.uid!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object:SingleObserver<Int>{
                                        override fun onSuccess(t: Int) {
                                            //Add items from previous command
                                            val cartItems = orderModel!!.
                                        }

                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onError(e: Throwable) {
                                            dialog.dismiss()
                                            Toast.makeText(context!!,e.message!!,Toast.LENGTH_SHORT).show()
                                        }
                                    })

                            }
                        }))
            }
        }
*/

    }

    override fun onLoadOrderSuccess(orderList: List<Order>) {
        dialog.dismiss()
        viewOrderModel!!.setMutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        compositeDisposable.clear()
        super.onDestroy()
    }


}