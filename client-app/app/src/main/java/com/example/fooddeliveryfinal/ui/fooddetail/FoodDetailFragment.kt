package com.example.fooddeliveryfinal.ui.fooddetail

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.fooddeliveryfinal.Common.Common
import com.example.fooddeliveryfinal.Database.CartDataSource
import com.example.fooddeliveryfinal.Database.CartDatabase
import com.example.fooddeliveryfinal.Database.CartItem
import com.example.fooddeliveryfinal.Database.LocalCartDataSource
import com.example.fooddeliveryfinal.EventBus.CountCartEvent
import com.example.fooddeliveryfinal.EventBus.MenuItemBack
import com.example.fooddeliveryfinal.Model.CommentModel
import com.example.fooddeliveryfinal.Model.FoodModel
import com.example.fooddeliveryfinal.R
import com.example.fooddeliveryfinal.R.id.btnCart
import com.example.fooddeliveryfinal.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_food_detail.*
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailFragment : Fragment(), TextWatcher {

    //Functions for the Addon option
    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(
        charSequence: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {

    }

    override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for (addonModel in Common.foodSelected!!.addon!!) {
            if (addonModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase())) {
                val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                chip.text = StringBuilder(addonModel.name!!).append(" (+ ").append(addonModel.price)
                    .append(" LEI )").toString()
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b)
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }
            }
        }
    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource: CartDataSource

    private lateinit var foodDetailModel: FoodDetailModel

    private lateinit var addonBottomSheetDialog: BottomSheetDialog

    private var img_food: ImageView? = null
    private var btnCart: CounterFab? = null
    private var btnRating: FloatingActionButton? = null
    private var food_name: TextView? = null
    private var food_description: TextView? = null
    private var food_price: TextView? = null
    private var number_button: ElegantNumberButton? = null
    private var ratingBar: RatingBar? = null
    private var btnShowComment: Button? = null
    private var rdi_group_size: RadioGroup? = null
    private var img_add_on: ImageView? = null
    private var chip_group_user_selected_addon: ChipGroup? = null

    //Addon layout
    private var chip_group_addon: ChipGroup? = null
    private var edt_search_addon: EditText? = null

    private var waitingDialog: AlertDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailModel =
            ViewModelProviders.of(this).get(FoodDetailModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_detail, container, false)
        initViews(root)


        foodDetailModel.getMutableLiveDataFood().observe(this, Observer {
            displayInfo(it)
        })

        foodDetailModel.getMutableLiveDataComment().observe(this, Observer {
            submitRatingToFirebase(it)
        })
        return root
    }

    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        //Comments REF
        FirebaseDatabase.getInstance()
            .getReference(Common.COMMENT_REF)
            .child(Common.foodSelected!!.id!!)
            .push()
            .setValue(commentModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    addRatingToFood(commentModel!!.ratingValue.toDouble())
                }
                waitingDialog!!.dismiss()
            }
    }

    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF) //Category selected
            .child(Common.categorySelected!!.menu_id!!) //Selected foods array
            .child("foods")
            .child(Common.foodSelected!!.key!!) //Select Key
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()
                    Toast.makeText(context!!, " " + p0.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(databaseSnapshot: DataSnapshot) {
                    if (databaseSnapshot.exists()) {
                        val foodModel = databaseSnapshot.getValue(FoodModel::class.java)
                        foodModel!!.key = Common.foodSelected!!.key

                        // Modify rating

                        val sumRating = foodModel.ratingValue.toDouble() + (ratingValue)
                        val ratingCount = foodModel.ratingCount + 1

                        val updateData = HashMap<String, Any>()
                        updateData["ratingValue"] = sumRating
                        updateData["ratingCount"] = ratingCount

                        //Update data in variable

                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = sumRating
                        databaseSnapshot.ref
                            .updateChildren(updateData)
                            .addOnCompleteListener { task ->
                                waitingDialog!!.dismiss()
                                if (task.isSuccessful) {
                                    Common.foodSelected = foodModel
                                    foodDetailModel!!.setFoodModel(foodModel)
                                    Toast.makeText(
                                        context!!,
                                        "Recenzia dumneavoastră a fost înregistrată ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else
                        waitingDialog!!.dismiss()
                }
            })
    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(context!!).load(it!!.image).into(img_food!!)
        food_name!!.text = StringBuilder(it!!.name!!)
        food_price!!.text = StringBuilder(it!!.price!!.toString())
        food_description!!.text = StringBuilder(it!!.description!!.toString())

        ratingBar!!.rating = it!!.ratingValue.toFloat() / it!!.ratingCount

        //Set size
        for (sizeModel in it!!.size) {
            val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener { buttonView: CompoundButton?, b: Boolean ->
                if (b)
                    Common.foodSelected!!.userSelectedSize = sizeModel
                calculateTotalPrice()
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            radioButton.layoutParams = params
            radioButton.text = sizeModel.name
            radioButton.tag = sizeModel.price

            rdi_group_size!!.addView(radioButton)

            //Which button is selected by default

            if (rdi_group_size!!.childCount > 0) {
                val radioButton = rdi_group_size!!.getChildAt(0) as RadioButton
                radioButton.isChecked = true
            }
        }
    }

    private fun calculateTotalPrice() {
        var totalPrice = Common.foodSelected!!.price.toDouble()
        var displayPrice = 0.0

        //Addon Calculator
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            for (addonModel in Common.foodSelected!!.userSelectedAddon!!)
                totalPrice += addonModel.price!!.toDouble()
        }
        //Size
        totalPrice += Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()

        displayPrice = totalPrice * number_button!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0) / 100.0

        food_price!!.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()
    }

    private fun initViews(root: View?) {

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!, R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display, null)
        chip_group_addon =
            layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener { dialog: DialogInterface? ->

            displayUserSelectedAddon()
            calculateTotalPrice()
        }


        waitingDialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        btnCart = root!!.findViewById(R.id.btnCart) as CounterFab
        img_food = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        food_name = root!!.findViewById(R.id.text_food_name) as TextView
        food_description = root!!.findViewById(R.id.food_description1) as TextView
        food_price = root!!.findViewById(R.id.food_price) as TextView
        number_button = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as Button
        rdi_group_size = root!!.findViewById(R.id.rdi_group_size) as RadioGroup
        img_add_on = root!!.findViewById(R.id.img_add_addon) as ImageView
        chip_group_user_selected_addon =
            root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup


        img_add_on!!.setOnClickListener {
            if (Common.foodSelected!!.addon != null) {
                displayAllAddon()
                addonBottomSheetDialog.show()
            }
        }

        //Event for rating and comment screen
            btnRating!!.setOnClickListener {
            showDialogRating()
            btnShowComment!!.setOnClickListener {
                val commentFragment = CommentFragment.getInstance()
                commentFragment.show(activity!!.supportFragmentManager, "CommentFragment")
            }
        }
        btnCart!!.setOnClickListener {

            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid
            cartItem.userPhone = Common.currentUser!!.phone

            cartItem.foodId = Common.foodSelected!!.id!!
            cartItem.foodName = Common.foodSelected!!.name!!
            cartItem.foodImage = Common.foodSelected!!.image!!
            cartItem.foodPrice = Common.foodSelected!!.price!!.toDouble()
            cartItem.foodQuantity = number_button!!.number.toInt()
            cartItem.foodExtraPrice = Common.calculateExtraPrice(
                Common.foodSelected!!.userSelectedSize,
                Common.foodSelected!!.userSelectedAddon
            )

            if (Common.foodSelected!!.userSelectedAddon != null)
                cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
            else
                cartItem.foodAddon = "Default"

            if (Common.foodSelected!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
            else
                cartItem.foodSize = "Default"

            cartDataSource.getItemwithAllOptionsInCart(
                Common.currentUser!!.uid!!,
                cartItem.foodId,
                cartItem.foodSize!!,
                cartItem.foodAddon!!
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<CartItem> {
                    override fun onSuccess(cartItemFromDB: CartItem) {
                        if (cartItemFromDB.equals(cartItem)) {
                            //If item in database, update
                            cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                            cartItemFromDB.foodAddon = cartItem.foodAddon
                            cartItem.foodSize = cartItem.foodSize
                            cartItemFromDB.foodQuantity =
                                cartItemFromDB.foodQuantity + cartItem.foodQuantity

                            cartDataSource.updateCart(cartItemFromDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : SingleObserver<Int> {
                                    override fun onSuccess(t: Int) {
                                        Toast.makeText(
                                            context,
                                            "Update Cart Success",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(
                                            context,
                                            "[EROARE LA MODIFICARE COȘ]" + e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else {
                            compositeDisposable.add(
                                cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Produs adăugat cu succes",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        //Notification to HomeActivity to update CounterFab(butonul de cart)
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[Adaugă în coș]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )
                        }
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty")) {
                            compositeDisposable.add(
                                cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Produs adăugat cu succes",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        //Notification to HomeActivity to update CounterFab(butonul de cart)
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[Adaugă în coș]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )

                        } else
                            Toast.makeText(
                                context,
                                "[EROARE LA ADĂUGAREA PRODUSULUI]" + e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                })
        }

    }

    private fun displayAllAddon() {
        if (Common.foodSelected!!.addon!!.size > 0) {
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()

            edt_search_addon!!.addTextChangedListener(this)

            for (addonModel in Common.foodSelected!!.addon!!) {

                val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+ ").append(addonModel.price)
                    .append(" LEI )").toString()
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b)
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }
                chip_group_addon!!.addView(chip)

            }

        }
    }

    private fun displayUserSelectedAddon() {

        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            chip_group_user_selected_addon!!.removeAllViews()
            for (addonModel in Common.foodSelected!!.userSelectedAddon!!) {
                val chip =
                    layoutInflater.inflate(R.layout.layout_chip_with_delete, null, false) as Chip
                chip.text =
                    StringBuilder(addonModel!!.name!!).append("(+LEI").append(addonModel.price)
                        .append(")").toString()
                chip.isClickable = false
                chip.setOnCloseIconClickListener { view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chip_group_user_selected_addon!!.addView(chip)
            }
        } else
            chip_group_user_selected_addon!!.removeAllViews()

    }

    private fun showDialogRating() {
        var builder = AlertDialog.Builder(context!!)

        builder.setTitle("Recenzia dumneavoastră")
        builder.setMessage("Vă rugăm completați toate câmpurile")

        //inflater for rating window
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment, null)

        val ratingBar = itemView.findViewById<RatingBar>(R.id.rating_bar)
        val edit_comment = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)

        builder.setNegativeButton("CANCEL") { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }

        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
            val commentModel = CommentModel()
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
            commentModel.comment = edit_comment.text.toString()
            commentModel.ratingValue = ratingBar.rating
            val serverTimeStamp = HashMap<String, Any>()
            serverTimeStamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = (serverTimeStamp)

            foodDetailModel.setCommentModel(commentModel)

        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}