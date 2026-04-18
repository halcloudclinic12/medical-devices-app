package com.test.healthbox_app.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.healthbox_app.databinding.ItemListDialogBinding
import com.test.healthbox_app.domain.model.StringValues
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ItemListDialog @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var dialog: Dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
    private lateinit var binding: ItemListDialogBinding

    private var valuesList: MutableList<StringValues>? = ArrayList<StringValues>()

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemSelectedListener(listener: OnItemClickListener) {
        this.mOnItemClickListener = listener
    }

    fun showDialog(label: String, itemLists: List<StringValues>) {

//        dialog.setCancelable(false)

        binding = ItemListDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.setCanceledOnTouchOutside(true)

        binding.tvHeader.text = label

        // Set fixed width and height
        dialog.window?.let { window ->
            // Set width to 80% of screen width
            val width = (context.resources.displayMetrics.widthPixels * 0.3).toInt()
            // Set height to 70% of screen height
            val height = (context.resources.displayMetrics.heightPixels * 0.6).toInt()

            window.setLayout(width, height)

            // Optional: Set dialog position to center
            window.setGravity(Gravity.CENTER)

            // Optional: Add animations
            // window.setWindowAnimations(R.style.DialogAnimation)
        }

        if (this.valuesList!!.isNotEmpty()) {
            this.valuesList!!.clear()
        }

        this.valuesList!!.addAll(itemLists)

        setupRecyclerView()

        Log.e("ItemsListLog", "  :   ${dialog.isShowing}   ::  $valuesList")

        if (!dialog.isShowing) dialog.show()

    }

    private fun setupRecyclerView() {

        binding.rvItemList.layoutManager = LinearLayoutManager(context)

        binding.rvItemList.adapter = valuesList?.let {
            ItemsListAdapter(it) { device ->
                print("ItemsDeviceAdapter:  $device")
                mOnItemClickListener?.onItemSelect(device)

                dialog.dismiss()
            }
        }
    }

    fun dismissDialog() {
        dialog.dismiss()
    }

    interface OnItemClickListener {
        fun onItemSelect(unit: StringValues?)
    }
}