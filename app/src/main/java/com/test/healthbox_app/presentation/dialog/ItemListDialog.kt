package com.test.healthbox_app.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
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

        binding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        // Width is fixed to a fraction of the screen; height is left to wrap the actual
        // item count instead of forcing a tall fixed box — a 2-item list (e.g. Plus/Minus)
        // used to be stretched to 60% of the screen height, leaving a large empty gap
        // below the last row.
        dialog.window?.let { window ->
            val width = (context.resources.displayMetrics.widthPixels * 0.34).toInt()

            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            window.setGravity(Gravity.CENTER)

            // The Dialog's own window decor paints an opaque rectangular background behind
            // whatever content view is set, so the rounded corners of item_list_dialog's
            // bg_solid_card showed a square grey box peeking out around them. Making the
            // window itself transparent leaves only the rounded card visible.
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Theme_Translucent_NoTitleBar_Fullscreen does not dim the screen behind the
            // dialog, so the calibration form's own white bg_solid_card panel sitting right
            // behind this (now much smaller, wrap-content) dialog stays fully visible and
            // peeks out around its edges — two white rounded shapes overlapping read as a
            // stray "grey rounded corner". Dimming behind the dialog, like every standard
            // Android dialog does, visually separates the two instead.
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.45f)
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