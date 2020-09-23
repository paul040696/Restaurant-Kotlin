package com.example.fooddeliveryserver.ui.category

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fooddeliveryserver.Adapters.MyCategoriesAdapter
import com.example.fooddeliveryserver.Callback.IMyButtonCallback
import com.example.fooddeliveryserver.Common.Common
import com.example.fooddeliveryserver.Common.MySwipeHelper
import com.example.fooddeliveryserver.Common.SpacesItemDecoration
import com.example.fooddeliveryserver.EventBus.ToastEvent
import com.example.fooddeliveryserver.Model.CategoryModel
import com.example.fooddeliveryserver.Model.FoodModel
import com.example.fooddeliveryserver.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CategoryFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter?=null

    private var recycler_menu: RecyclerView?=null

    internal var categoryModels: List<CategoryModel> = ArrayList<CategoryModel>()
    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    internal lateinit var img_category:ImageView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        categoryViewModel =
            ViewModelProviders.of(this).get(CategoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)

        initViews(root)

        categoryViewModel.getMessageError().observe(this,Observer {
            Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })

        categoryViewModel.getCategoryList().observe(this, Observer {
            dialog.dismiss()
            categoryModels = it
            adapter = MyCategoriesAdapter(context!!,categoryModels)
            recycler_menu!!.adapter = adapter
            recycler_menu!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initViews(root:View) {

        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        recycler_menu = root.findViewById(R.id.recycler_menu) as RecyclerView
        recycler_menu!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recycler_menu!!.layoutManager = layoutManager
        recycler_menu!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object : MySwipeHelper(context!!, recycler_menu!!, 200) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Modifică",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {


                                Common.categorySelected = categoryModels[pos]

                                showUpdateDialog()

                            }
                        })
                )
            }

        }

        setHasOptionsMenu(true)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.action_bar_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_create){
            showAddDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Modifică categorie")
        builder.setMessage("Vă rugăm completați informațiile")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edt_category_name = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        img_category = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Seting data for items
        edt_category_name.setText(Common.categorySelected!!.name)
        Glide.with(context!!).load(Common.categorySelected!!.image).into(img_category)

        //Seting events
        img_category.setOnClickListener { view ->
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Selectează o fotografie"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("ANULEAZĂ") {dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("MODIFICĂ") {dialogInterface, i ->


            val updateData = HashMap<String,Any>()
            updateData["name"]=edt_category_name.text.toString()
            if(imageUri != null)
            {
                    dialog.setMessage("Se încarcă....")
                    dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener { e ->
                        dialog.dismiss()
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        dialog.setMessage("Încărcat $progress%")
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        dialog.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            updateData["image"] = uri.toString()
                            updateCategory(updateData)
                        }
                    }
            }
            else
            {
                updateCategory(updateData)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun updateCategory(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener { e-> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener { task ->
                categoryViewModel!!.loadCategory()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.UPDATE,false))

            }
    }

    private fun showAddDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Crează o categorie nouă")
        builder.setMessage("Vă rugăm completați informațiile")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edt_category_name = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        img_category = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Seting data for items
        Glide.with(context!!).load(R.drawable.ic_image_gray_24dp).into(img_category)

        //Seting events
        img_category.setOnClickListener { view ->
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Selectează o fotografie"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("ANULEAZĂ") {dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("CREAZĂ") {dialogInterface, i ->



            val categoryModel = CategoryModel()
            categoryModel.name = edt_category_name.text.toString()
            categoryModel.foods = ArrayList<FoodModel>()


            if(imageUri != null)
            {
                dialog.setMessage("Se încarcă....")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener { e ->
                        dialog.dismiss()
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        dialog.setMessage("Încărcat $progress%")
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        dialog.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            categoryModel.image = uri.toString()
                            addCategory(categoryModel)
                        }
                    }
            }
            else
            {
                addCategory(categoryModel)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun addCategory(categoryModel: CategoryModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .push()
            .setValue(categoryModel)
            .addOnFailureListener { e-> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener { task ->
                categoryViewModel!!.loadCategory()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.CREATE,false))

            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data != null && data.data !=null )
            {
                imageUri = data.data
                img_category.setImageURI(imageUri)

            }
        }
    }


}