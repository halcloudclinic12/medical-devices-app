package com.test.healthbox_app.presentation.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.domain.model.StringValues
import javax.inject.Inject

class ItemsListAdapter @Inject constructor(
    private val itemLists: List<StringValues>,
    private val onItemClick: (StringValues) -> Unit
) : RecyclerView.Adapter<ItemsListAdapter.ItemsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selection_layout, parent, false)
        return ItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemLists.size
    }

    override fun onBindViewHolder(holder: ItemsViewHolder, position: Int) {
        val items = itemLists[position]
        holder.bind(items, onItemClick)
    }

    class ItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemLayoutCl: ConstraintLayout = itemView.findViewById(R.id.item_layout_cl)
        private val tvItemName: TextView = itemView.findViewById(R.id.tv_item_name)
        private val isSelectedRadioButton: RadioButton = itemView.findViewById(R.id.is_selected_radio_button)

        fun bind(item: StringValues, onItemClick: (StringValues) -> Unit) {

            tvItemName.text = item.value
            isSelectedRadioButton.isChecked = item.isSelected!!

            itemLayoutCl.setOnClickListener { view ->
                print("onItemClickLogs  : $item")

                onItemClick(item)
            }
        }
    }
}