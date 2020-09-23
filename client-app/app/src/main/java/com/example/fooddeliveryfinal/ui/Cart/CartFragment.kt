package com.example.fooddeliveryfinal.ui.Cart

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.example.fooddeliveryfinal.Adapter.MyCartAdapter
import com.example.fooddeliveryfinal.Callback.ILoadTimeFromFirebaseCall
import com.example.fooddeliveryfinal.Callback.IMyButtonCallback
import com.example.fooddeliveryfinal.Common.Common
import com.example.fooddeliveryfinal.Common.MySwipeHelper
import com.example.fooddeliveryfinal.Database.CartDataSource
import com.example.fooddeliveryfinal.Database.CartDatabase
import com.example.fooddeliveryfinal.Database.CartItem
import com.example.fooddeliveryfinal.Database.LocalCartDataSource
import com.example.fooddeliveryfinal.EventBus.CountCartEvent
import com.example.fooddeliveryfinal.EventBus.HideFABCart
import com.example.fooddeliveryfinal.EventBus.MenuItemBack
import com.example.fooddeliveryfinal.EventBus.UpdateItemInCart
import com.example.fooddeliveryfinal.Model.Order
import com.example.fooddeliveryfinal.R
import com.example.fooddeliveryfinal.Remote.ICloudFunctions
import com.example.fooddeliveryfinal.Remote.RetrofitCloudClient
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class CartFragment : Fragment(), ILoadTimeFromFirebaseCall {

    override fun onLoadTimeSucces(order: Order, estimatedTimeMs: Long) {
        order.createDate = estimatedTimeMs
        order.orderStatus = 0
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context!!, message, Toast.LENGTH_SHORT).show()
    }

    private val REQUEST_BRAINTREE_CODE: Int = 8898
    private var cartDataSource: CartDataSource? = null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var recyclerViewState: Parcelable? = null
    private lateinit var cartViewModel: CartViewModel
    private lateinit var btn_place_order: Button

    var txt_empty_cart: TextView? = null
    var txt_total_price: TextView? = null
    var group_place_holder: CardView? = null
    var recycler_cart: RecyclerView? = null
    var adapter: MyCartAdapter? = null

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    internal var address: String = ""
    internal var comment: String = ""

    lateinit var cloudFunctions: ICloudFunctions

    lateinit var listener: ILoadTimeFromFirebaseCall

    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        EventBus.getDefault().postSticky(HideFABCart(true))

        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)
        //Init data source
        cartViewModel.initCartdataSource(context!!)

        val root = inflater.inflate(R.layout.fragment_cart, container, false)
        initViews(root)
        initLocation()

        //vizibilitate cos
        cartViewModel.getMutableLiveDataCartItem().observe(this, Observer {
            if (it == null || it.isEmpty()) {
                recycler_cart!!.visibility = View.GONE
                group_place_holder!!.visibility = View.GONE
                txt_empty_cart!!.visibility = View.VISIBLE
            } else {
                recycler_cart!!.visibility = View.VISIBLE
                group_place_holder!!.visibility = View.VISIBLE
                txt_empty_cart!!.visibility = View.GONE

                adapter = MyCartAdapter(context!!, it)
                recycler_cart!!.adapter = adapter
            }
        })
        return root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)

    }

    private fun initViews(root: View) {

        setHasOptionsMenu(true)  //to inflate menu

        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        listener = this

        recycler_cart = root.findViewById(R.id.recycler_Cart) as RecyclerView
        recycler_cart!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recycler_cart!!.layoutManager = layoutManager
        recycler_cart!!.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

        val swipe = object : MySwipeHelper(context!!, recycler_cart!!, 200) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Elimină",
                        30,
                        0,
                        Color.parseColor("#FF3c30"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                Toast.makeText(context, "Delete Item", Toast.LENGTH_SHORT).show()
                                val deteleItem = adapter!!.getItemAtPosition(pos)
                                cartDataSource!!.deleteCart(deteleItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSuccess(t: Int) {
                                            adapter!!.notifyItemRemoved(pos)
                                            sumCart()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                            Toast.makeText(
                                                context,
                                                "Produs șters cu success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onSubscribe(d: Disposable) {
                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "" + e.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            }
                        })
                )
            }
        }

        txt_empty_cart = root.findViewById(R.id.txt_empty_cart) as TextView
        txt_total_price = root.findViewById(R.id.txt_total_price) as TextView
        group_place_holder = root.findViewById(R.id.group_place_holder) as CardView

        btn_place_order = root.findViewById(R.id.btn_place_order) as Button

        //Plaseaza comanda
        btn_place_order.setOnClickListener {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Detalii livrare")

            val view = LayoutInflater.from(context).inflate(R.layout.layout_place_order, null)

            val edt_adress = view.findViewById<View>(R.id.edt_address) as EditText
            val edt_comment = view.findViewById<View>(R.id.edt_comment) as EditText
            val txt_adress = view.findViewById<View>(R.id.txt_address_detail) as TextView
            val rdi_home = view.findViewById<View>(R.id.rd_home_address) as RadioButton
            val rdi_other_address = view.findViewById<View>(R.id.rd_other_address) as RadioButton
            val rdi_ship_to_address = view.findViewById<View>(R.id.rd_ship_this_address) as RadioButton
            val rdi_cash = view.findViewById<View>(R.id.rd_cash) as RadioButton
            val rdi_card = view.findViewById<View>(R.id.rd_card) as RadioButton

            //Data
            edt_adress.setText(Common.currentUser!!.address!!) // User's address defined in account

            //Event
            rdi_home.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    edt_adress.setText(Common.currentUser!!.address!!)
                    txt_adress.visibility = View.GONE
                }
            }
            rdi_other_address.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    edt_adress.setText("")
                    edt_adress.setHint("Introduceți adresa dvs")
                    txt_adress.visibility = View.GONE
                }
            }
            rdi_ship_to_address.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e ->
                            txt_adress.visibility = View.GONE
                            Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { task ->
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()

                            val singleAddress = Single.just(
                                getAddressFromLatLng(
                                    task.result!!.latitude,
                                    task.result!!.longitude
                                )
                            )

                            val
                                    disposable = singleAddress.subscribeWith(object :
                                DisposableSingleObserver<String>() {
                                override fun onSuccess(t: String) {
                                    edt_adress.setText(coordinates)
                                    txt_adress.visibility = View.VISIBLE
                                    txt_adress.setText(t)
                                }

                                override fun onError(e: Throwable) {
                                    edt_adress.setText(coordinates)
                                    txt_adress.visibility = View.VISIBLE
                                    txt_adress.setText(e.message!!)
                                }
                            })
                        }
                }
            }

            builder.setView(view)
            builder.setNegativeButton("NU", { dialogInterface, _ -> dialogInterface.dismiss() })
                .setPositiveButton("DA",
                    { dialogInterface, _ ->
                        if (rdi_cash.isChecked)
                            paymentCOD(edt_adress.text.toString(), edt_comment.text.toString())
                        else if (rdi_card.isChecked) {
                            address = edt_adress.text.toString()
                            comment = edt_comment.text.toString()
                            if (!TextUtils.isEmpty(Common.currentToken)) {
                                val dropInRequest = DropInRequest().clientToken(Common.currentToken)
                                startActivityForResult(
                                    dropInRequest.getIntent(context),
                                    REQUEST_BRAINTREE_CODE
                                )
                            }
                        }
                    })

            val dialog = builder.create()
            dialog.show()

        }
    }

    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.add(
            cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItemList ->
                    cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Double> {
                            override fun onSuccess(totalPrice: Double) {
                                val finalPrice = totalPrice
                                val order = Order()
                                order.userId = Common.currentUser!!.uid!!
                                order.userName = Common.currentUser!!.name!!
                                order.userPhone = Common.currentUser!!.phone!!
                                order.shippingAddress = address
                                order.comment = comment

                                if (currentLocation != null) {
                                    order.lat = currentLocation!!.latitude
                                    order.lng = currentLocation!!.longitude
                                }

                                order.cartItemList = cartItemList
                                order.totalPayment = totalPrice
                                order.finalPayment = finalPrice
                                order.discount = 0
                                order.isCod = true
                                order.transactionId = "Cash la livrare"

                                //Submit to Firebase
                                syncLocalTimeWithServerTime(order)
                            }

                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                            }

                        })
                },
                    { throwable ->
                        Toast.makeText(
                            context!!,
                            "" + throwable.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    private fun writeOrderToFirebase(order: Order) {

        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e ->
                Toast.makeText(
                    context!!,
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task ->
                //Clean cart if (task.isSuccessful) {
                cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : SingleObserver<Int> {
                        override fun onSuccess(t: Int) {
                            Toast.makeText(
                                context!!,
                                "Comanda dvs a fost plasată cu succes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                        }
                    })
            }
    }


    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context!!, Locale.getDefault())
        var result: String? = null
        try {
            val addressList = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null && addressList.size > 0) {
                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            } else
                result = "Adresa nu a fost gastiă"
            return result
        } catch (e: IOException) {
            return e.message!!
        }
    }

    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSuccess(t: Double) {
                    txt_total_price!!.text = StringBuilder("Total: LEI")
                        .append(t)
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (e!!.message!!.contains("Query returned empty"))
                        Toast.makeText(context, "" + e.message!!, Toast.LENGTH_SHORT).show()
                }

            });

    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        cartViewModel!!.onStop()
        compositeDisposable.clear()

        EventBus.getDefault().postSticky(HideFABCart(false))
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event: UpdateItemInCart) {
        if (event.cartItem != null) {
            recyclerViewState = recycler_cart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice()
                        recycler_cart!!.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "[MODIFICARE COȘ]" + e.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSuccess(price: Double) {
                    txt_total_price!!.text = StringBuilder("Total: LEI")
                        .append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(
                            context,
                            "[Total Calculat]" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                }

            })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings).setVisible(false) //Hide Settings menu in cart
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == R.id.action_clear_cart) {
            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSuccess(t: Int) {
                        Toast.makeText(context, "Coș golit cu success", Toast.LENGTH_SHORT).show()
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                })
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BRAINTREE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result =
                    data!!.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)
                val nonce = result!!.paymentMethodNonce

                //sum of cart
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Double> {
                        override fun onSuccess(totalPrice: Double) {

                            compositeDisposable.add(
                                cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ cartItems: List<CartItem>? ->

                                        val headers = HashMap<String, String>()
                                        headers.put(
                                            "Authorization",
                                            Common.buildToken(Common.autorizeToken!!)
                                        )

                                        compositeDisposable.add(
                                            cloudFunctions.submitPayment(
                                                headers,
                                                totalPrice,
                                                nonce!!.nonce
                                            )
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe({ braintreeTransaction ->
                                                    if (braintreeTransaction.success) {
                                                        //Create order
                                                        val finalPrice = totalPrice
                                                        val order = Order()
                                                        order.userId = Common.currentUser!!.uid!!
                                                        order.userName = Common.currentUser!!.name!!
                                                        order.userPhone =
                                                            Common.currentUser!!.phone!!
                                                        order.shippingAddress = address
                                                        order.comment = comment

                                                        if (currentLocation != null) {
                                                            order.lat = currentLocation!!.latitude
                                                            order.lng = currentLocation!!.longitude
                                                        }

                                                        order.cartItemList = cartItems
                                                        order.totalPayment = totalPrice
                                                        order.finalPayment = finalPrice
                                                        order.discount = 0
                                                        order.isCod = false
                                                        order.transactionId =
                                                            braintreeTransaction.transaction!!.id

                                                        //Submit to Firebase
                                                        syncLocalTimeWithServerTime(order)

                                                    }
                                                },
                                                    { t: Throwable ->
                                                        Toast.makeText(
                                                            context,
                                                            "" + t!!.message,
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    })
                                        )

                                    }, { t: Throwable ->

                                        Toast.makeText(
                                            context,
                                            "" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                        }

                    })
            }
        }
    }

    private fun syncLocalTimeWithServerTime(order: Order) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis() + offset!!
                val sdf = SimpleDateFormat("MMM dd yyyy HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("Paul", "" + sdf.format(date))
                listener.onLoadTimeSucces(order, estimatedServerTimeInMs)
            }

        })
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}