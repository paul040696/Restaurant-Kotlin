package com.example.fooddeliveryfinal

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import com.example.fooddeliveryfinal.Database.CartDataSource
import com.example.fooddeliveryfinal.Database.CartDatabase
import com.example.fooddeliveryfinal.Database.LocalCartDataSource
import com.example.fooddeliveryfinal.EventBus.*
import com.example.fooddeliveryfinal.Model.CategoryModel
import com.example.fooddeliveryfinal.Model.FoodModel
import com.example.fooddeliveryfinal.Model.PopularCategoryModel
import com.google.android.gms.common.internal.service.Common
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.single.SingleObserveOn
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var   cartDataSource:CartDataSource
    private lateinit var navController:NavController
    private var drawer:DrawerLayout?=null
    private var dialog:AlertDialog?= null

    private var menuItemClick = 1


    override fun onResume() {
        super.onResume()
        countCartItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
           navController.navigate(R.id.nav_cart)
        }
        drawer = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
         navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detial,
                R.id.nav_cart
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.bringToFront();

        var headerView = navView.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        com.example.fooddeliveryfinal.Common.Common.setSpanString("Bună, ",com.example.fooddeliveryfinal.Common.Common.currentUser!!.name,txt_user)


        //Sign Out
        navView.setNavigationItemSelectedListener(object: NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                p0.isChecked = true
                drawer!!.closeDrawers()


                if(p0.itemId == R.id.nav_sign_out)
                {
                    signOut()
                }
                else if(p0.itemId == R.id.nav_home)
                {
                    if(menuItemClick!= p0.itemId)
                        navController.navigate(R.id.nav_home)
                }
                else if(p0.itemId == R.id.nav_cart)
                {
                    if(menuItemClick!= p0.itemId)
                        navController.navigate(R.id.nav_cart)
                }
                else if(p0.itemId == R.id.nav_menu)
                {
                    if(menuItemClick!= p0.itemId)
                        navController.navigate(R.id.nav_menu)
                }
                else if(p0.itemId == R.id.nav_view_order)
                {
                    if(menuItemClick!= p0.itemId)
                        navController.navigate(R.id.nav_view_order)
                }

                menuItemClick = p0!!.itemId
                return true
            }
        })

        countCartItem()
    }

    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Deconectare")
            .setMessage("Sunteți sigur că doriți să vă deconectați?")
            .setNegativeButton("Cancel",{dialogInterface,_ -> dialogInterface.dismiss()})
            .setPositiveButton("OK"){dialogInterface,_ ->
                com.example.fooddeliveryfinal.Common.Common.foodSelected = null
                com.example.fooddeliveryfinal.Common.Common.categorySelected = null
                com.example.fooddeliveryfinal.Common.Common.currentUser = null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity,MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            }

        val dialog = builder.create()
        dialog.show()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event:CategoryClick)
    {
        if(event.isSuccess)
        {
            //Toast.makeText(this,"Click to "+event.category.name,Toast.LENGTH_SHORT).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FoodItemClick)
    {
        if(event.isSuccess)
        {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detial)
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onHideFABEvent(event: HideFABCart)
    {
        if(event.isHide)
        {
            fab.hide()
        }
        else
            fab.show()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event: CountCartEvent)
    {
        if(event.isSuccess)
        {
            countCartItem()
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event: PopularFoodItemClick)
    {
        if(event.popularCategoryModel != null)
        {
            dialog!!.show()
            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.popularCategoryModel!!.menu_id!!)
                .addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                        }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists())
                        {
                            com.example.fooddeliveryfinal.Common.Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            com.example.fooddeliveryfinal.Common.Common.categorySelected!!.menu_id = p0.key
                            //Load food
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.popularCategoryModel!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.popularCategoryModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object :ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if(p0.exists())
                                        {
                                            for(foodSnapshot in p0.children)
                                            {
                                                com.example.fooddeliveryfinal.Common.Common.foodSelected = foodSnapshot.getValue(FoodModel::class.java)
                                                com.example.fooddeliveryfinal.Common.Common.foodSelected!!.key =foodSnapshot.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detial)
                                        }
                                        else
                                        {
                                            dialog!!.dismiss()
                                            Toast.makeText(this@HomeActivity,"Produsul nu exista",Toast.LENGTH_SHORT).show()
                                        }
                                        dialog!!.dismiss()
                                    }
                                })
                        }
                        else
                        {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,"Produsul nu exista",Toast.LENGTH_SHORT).show()
                        }
                    }

                })
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onBestDealFoodItemClick(event: BestDealItemClick)
    {
        if(event.model != null)
        {
            dialog!!.show()
            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.model!!.menu_id!!)
                .addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists())
                        {
                            com.example.fooddeliveryfinal.Common.Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            com.example.fooddeliveryfinal.Common.Common.categorySelected!!.menu_id = p0.key
                            //Load food
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.model!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.model.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object :ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if(p0.exists())
                                        {
                                            for(foodSnapshot in p0.children) {
                                                com.example.fooddeliveryfinal.Common.Common.foodSelected =
                                                    foodSnapshot.getValue(FoodModel::class.java)
                                                com.example.fooddeliveryfinal.Common.Common.foodSelected!!.key = foodSnapshot.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detial)
                                        }
                                        else
                                        {
                                            dialog!!.dismiss()
                                            Toast.makeText(this@HomeActivity,"Produsul nu exista",Toast.LENGTH_SHORT).show()
                                        }
                                        dialog!!.dismiss()
                                    }
                                })
                        }
                        else
                        {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,"Produsul nu exista",Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public fun onMenuItemBack(event:MenuItemBack)
    {
        menuItemClick = -1
        if(supportFragmentManager.backStackEntryCount >0)
            supportFragmentManager.popBackStack();
    }
    private fun countCartItem() {
            cartDataSource.countItemInCart(com.example.fooddeliveryfinal.Common.Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        fab.count = t
                    }
                    override fun onSubscribe(d: Disposable) {
                    }
                    override fun onError(e: Throwable) {
                        if (!e.message!!.contains("Query returned empty"))
                            Toast.makeText(this@HomeActivity,"[COUNT CART]"+e.message,Toast.LENGTH_SHORT).show()
                        else
                            fab.count = 0
                    }
                })
        }
}
