package com.example.myapplication.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.myapplication.ListSerialItem
import com.example.myapplication.R

class ListSerialMonitorAdapter(
    val mainActivity: Activity,
    val serialItemList: ArrayList<ListSerialItem>
) : RecyclerView.Adapter<ListSerialMonitorHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListSerialMonitorHolder {
        return ListSerialMonitorHolder(
            LayoutInflater.from(parent.context).inflate( R.layout.item_serial_monitor, parent,  false)
        )
    }

    override fun getItemCount(): Int {
        return serialItemList.size
    }

    override fun onBindViewHolder(holder: ListSerialMonitorHolder, position: Int) {
        val data = serialItemList.get(position)

//        with(holder) {
//            txtTime.setText(data.time)
//            txtMsg.setText(data.data)
//            txtMsg.setTextColor(data.color)
//        }
    }
}

class ListSerialMonitorHolder(itemHolder : View) : ViewHolder(itemHolder) {
//    var txtTime = itemHolder.findViewById<TextView>(R.id.txtTime)
//    var txtMsg = itemHolder.findViewById<TextView>(R.id.txtMsg)
}
