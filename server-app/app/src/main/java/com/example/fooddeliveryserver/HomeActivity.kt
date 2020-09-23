package com.example.fooddeliveryserver

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
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.EventBus.CategoryClick
import com.example.fooddeliveryserver.EventBus.ChangeMenuClick
import com.example.fooddeliveryserver.EventBus.ToastEvent
import com.google.firebase.auth.FirebaseAuth
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private var menuClick: Int = -1
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout:DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category, R.id.nav_food_list,R.id.nav_order
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        navView.setNavigationItemSelectedListener(object: NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                // Navigare prin meniul aplicatiei
                p0.isChecked = true
                drawerLayout!!.closeDrawers()

                if(p0.itemId == R.id.nav_sign_out)
                {
                    signOut()
                }
                else if(p0.itemId == R.id.nav_category)
                {
                    if(menuClick!= p0.itemId)
                    {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_category)
                    }
                }
                else if(p0.itemId == R.id.nav_order)
                {
                    if(menuClick!= p0.itemId)
                    {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_order)

                    }
                }
                menuClick = p0!!.itemId
                return true
            }
        })


        val headerView = navView.getHeaderView(0)
        val txt_user = headerView.findViewById<View>(R.id.txt_user) as TextView
        Common.setSpanString("Bună, ",Common.currentServerUser!!.name,txt_user)

        menuClick = R.id.nav_food_list // By default
    }


    //deconectare utilizator curent
    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Deconectare")
            .setMessage("Sunteți sigur că doriți să vă deconectați?")
            .setNegativeButton("Cancel",{dialogInterface,_ -> dialogInterface.dismiss()})
            .setPositiveButton("OK"){dialogInterface,_ ->
                com.example.fooddeliveryserver.Common.Common.foodSelected = null
                com.example.fooddeliveryserver.Common.Common.categorySelected = null
                com.example.fooddeliveryserver.Common.Common.currentServerUser = null
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

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCategoryClick(event:CategoryClick)
    {
        if(event.isSuccess)
        {
            if(menuClick != R.id.nav_food_list)
            {
                navController!!.navigate(R.id.nav_food_list)
                menuClick = R.id.nav_food_list
            }
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onChangeMenuEvent(event:ChangeMenuClick)
    {
       if(!event.isFromFoodList)
       {
           navController!!.popBackStack(R.id.nav_category,true)
           navController!!.navigate(R.id.nav_category)
       }
       menuClick = -1
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onToastEvent(event:ToastEvent)
    {
        if(event.action == Common.ACTION.CREATE)
        {
            Toast.makeText(this,"Creat cu succes",Toast.LENGTH_SHORT).show()
        }
        else if(event.action == Common.ACTION.UPDATE)
        {
            Toast.makeText(this,"Modificare cu succes",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this,"Eliminat cu succes",Toast.LENGTH_SHORT).show()
        }
        EventBus.getDefault().postSticky(ChangeMenuClick(event.isBackFromFoodList))
    }

}

