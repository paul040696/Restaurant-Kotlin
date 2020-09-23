package com.example.fooddeliveryserver.ui.Order

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fooddeliveryserver.Adapters.MyOrderAdapter
import com.example.fooddeliveryserver.Callback.IMyButtonCallback
import com.example.fooddeliveryserver.Common.BottomSheetOrderFragment
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.Common.MySwipeHelper
import com.example.fooddeliveryserver.EventBus.AddonSizeEditEvent
import com.example.fooddeliveryserver.EventBus.ChangeMenuClick
import com.example.fooddeliveryserver.EventBus.LoadOrderEvent
import com.example.fooddeliveryserver.Model.OrderModel
import com.example.fooddeliveryserver.R
import com.example.fooddeliveryserver.SizeAddonActivity
import com.google.firebase.database.FirebaseDatabase
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.fragment_order.*
import kotlinx.android.synthetic.main.layout_order_item.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class OrderFragment : Fragment() {

    lateinit var recycler_order: RecyclerView
    lateinit var layoutAnimationController: LayoutAnimationController

    lateinit var orderViewModel: OrderViewModel

    private var adapter: MyOrderAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_order, container, false)
        initViews(root)

        orderViewModel = ViewModelProviders.of(this).get(OrderViewModel::class.java)

        orderViewModel!!.messageError.observe(this, Observer { s ->
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        })
        orderViewModel!!.getOrderModelList().observe(this, Observer { orderList ->
            if (orderList != null) {
                adapter = MyOrderAdapter(context!!, orderList.toMutableList())
                recycler_order.adapter = adapter
                recycler_order.layoutAnimation = layoutAnimationController

                updateTextCounter()
            }
        })

        return root
    }

    private fun initViews(root: View) {

        setHasOptionsMenu(true)

        recycler_order = root.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        recycler_order.layoutManager = LinearLayoutManager(context)

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object : MySwipeHelper(context!!, recycler_order!!, width / 6) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
               /* buffer.add(
                    MyButton(context!!,
                        "Direcții",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {

                            }
                        })
                )
*/
                buffer.add(
                    MyButton(context!!,
                        "Modifică",
                        30,
                        0,
                        Color.parseColor("#9b0000"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {

                               showEditDialog(adapter!!.getItemAtPosition(pos),pos)

                            }
                        })
                )
                buffer.add(
                    MyButton(context!!,
                        "Sună",
                        30,
                        0,
                        Color.parseColor("#12005e"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {

                                Dexter.withActivity(activity)
                                    .withPermission(android.Manifest.permission.CALL_PHONE)
                                    .withListener(object : PermissionListener {

                                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                                            val orderModel = adapter!!.getItemAtPosition(pos)
                                            val intent = Intent()
                                            intent.setAction(Intent.ACTION_DIAL)
                                            intent.setData(Uri.parse(StringBuilder("tel: ")
                                                .append(orderModel.userPhone).toString()))
                                            startActivity(intent)
                                        }

                                        override fun onPermissionRationaleShouldBeShown(
                                            permission: PermissionRequest?,
                                            token: PermissionToken?
                                        ) {
                                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                                        }

                                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                                            Toast.makeText(
                                                context,
                                                "Permisiune neacceptata" + response!!.permissionName,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }).check()
                            }
                        })
                )

                buffer.add(
                    MyButton(context!!,
                        "Elimină",
                        30,
                        0,
                        Color.parseColor("#333639"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {

                                val orderModel = adapter!!.getItemAtPosition(pos)

                                val builder = AlertDialog.Builder(context!!)
                                    .setTitle("Șterge")
                                    .setMessage("Doriți să eliminați această comandă?")
                                    .setNegativeButton("Anulează"){dialogInterface, i -> dialogInterface.dismiss() }
                                    .setPositiveButton("Șterge"){dialogInterface, i ->
                                        FirebaseDatabase.getInstance()
                                            .getReference(Common.ORDER_REF)
                                            .child(orderModel!!.key!!)
                                            .removeValue()
                                            .addOnFailureListener {
                                                Toast.makeText(context!!,""+it.message,Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnSuccessListener {
                                                adapter!!.removeItem(pos)
                                                adapter!!.notifyItemRemoved(pos)
                                                updateTextCounter()
                                                dialogInterface.dismiss()
                                                Toast.makeText(context!!,"Comanda a fost eliminată cu succes",Toast.LENGTH_SHORT).show()

                                            }
                                    }

                                val dialog = builder.create()
                                dialog.show()

                                val btn_negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                                btn_negative.setTextColor(Color.LTGRAY)
                                val btn_positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                btn_positive.setTextColor(Color.RED)
                            }
                        })
                )
            }
        }
    }

    private fun showEditDialog(orderModel: OrderModel, pos: Int) {

        var layout_dialog:View?=null
        var builder:AlertDialog.Builder?=null

        var rdi_shipping:RadioButton?=null
        var rdi_cancelled:RadioButton?=null
        var rdi_shipped:RadioButton?=null

        var rdi_delete:RadioButton?=null
        var rdi_restore_placed:RadioButton?=null

        if(orderModel.orderStatus == -1 )
        {
            layout_dialog = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_dialog_cancelled,null)
            builder = AlertDialog.Builder(context!!)
                .setView(layout_dialog)


             rdi_delete = layout_dialog.findViewById<View>(R.id.rdi_delete) as RadioButton
             rdi_restore_placed = layout_dialog.findViewById<View>(R.id.rdi_restore) as RadioButton


        }
        else if(orderModel.orderStatus == 0)
        {
            layout_dialog = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_dialog_shipping,null)
            builder = AlertDialog.Builder(context!!)
                .setView(layout_dialog)

            rdi_shipping = layout_dialog.findViewById<View>(R.id.rdi_shipping) as RadioButton
            rdi_cancelled = layout_dialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton

        }
        else
        {
            layout_dialog = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_dialog_shipped,null)
            builder = AlertDialog.Builder(context!!)
                .setView(layout_dialog)

            rdi_shipped = layout_dialog.findViewById<View>(R.id.rdi_shipped) as RadioButton
            rdi_cancelled = layout_dialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
        }

        //View
        val btn_ok = layout_dialog.findViewById<View>(R.id.btn_ok) as Button
        val btn_cancel = layout_dialog.findViewById<View>(R.id.btn_cancel) as Button


        val txt_status = layout_dialog.findViewById<View>(R.id.txt_status) as TextView

        //Setam datele

        txt_status.setText(StringBuilder("Status comanda(")
            .append(Common.converStatusToString(orderModel.orderStatus))
            .append(")"))

        val dialog = builder.create()
        dialog.show()
        btn_cancel.setOnClickListener { dialog.dismiss() }
        btn_ok.setOnClickListener {
            dialog.dismiss()
            if(rdi_cancelled !=null && rdi_cancelled.isChecked)
            {
                updateOrder(pos,orderModel,-1)
            }
            else if(rdi_shipping !=null && rdi_shipping.isChecked)
            {
                updateOrder(pos,orderModel,1)
            }
            else if(rdi_shipped !=null && rdi_shipped.isChecked)
            {
                updateOrder(pos,orderModel,2)
            }
            else if(rdi_restore_placed !=null && rdi_restore_placed.isChecked)
            {
                updateOrder(pos,orderModel,0)
            }
            else if(rdi_delete !=null && rdi_delete.isChecked)
            {
                deleteOrder(pos,orderModel)
            }
        }

    }

    private fun deleteOrder(pos: Int, orderModel: OrderModel) {
        if(!TextUtils.isEmpty(orderModel.key))
        {

            FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .removeValue()
                .addOnFailureListener { throwable -> Toast.makeText(context!!,""+throwable.message,
                    Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()
                    Toast.makeText(context!!,"Comanda ștearsă cu succes",
                        Toast.LENGTH_SHORT).show()
                }
        }
        else
        {
            Toast.makeText(context!!,"Comandă trebuie să conțină produse sau să nu fie null",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrder(pos: Int, orderModel: OrderModel, status: Int) {
        if(!TextUtils.isEmpty(orderModel.key))
        {
            val update_data = HashMap<String,Any>()
            update_data.put("orderStatus",status)

            FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .updateChildren(update_data)
                .addOnFailureListener { throwable -> Toast.makeText(context!!,""+throwable.message,
                    Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()
                    Toast.makeText(context!!,"Comanda actualizată cu succes",
                        Toast.LENGTH_SHORT).show()
                }
        }
        else
        {
            Toast.makeText(context!!,"Comandă trebuie să conțină produse sau să nu fie null",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTextCounter() {
        txt_order_filter.setText(
            StringBuilder("Comenzi (")
                .append(adapter!!.itemCount)
                .append(")")
        )
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_filter) {
            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(activity!!.supportFragmentManager, "Lista comenzi")
            return true
        }
        else
            return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent::class.java)
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)


        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onLoadOrder(event: LoadOrderEvent) {
        orderViewModel.loadOrder(event.status)
    }

}