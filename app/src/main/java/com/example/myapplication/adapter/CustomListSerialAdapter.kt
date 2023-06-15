package com.example.myapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.models.SerialItem
import java.util.ArrayList
import java.util.Locale

class CustomListSerialAdapter(contect : Context, val items : ArrayList<SerialItem>) : ArrayAdapter<SerialItem>(contect, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItem = items.get(position)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.device_list_item, parent, false)

        val item = getItem(position)

        var text1: TextView = view.findViewById(R.id.t1)
        var text2: TextView = view.findViewById(R.id.t2)

        if (listItem.driver == null)
            text1.setText("<no driver")
        else if(listItem.driver!!.ports.size == 1)
            text1.text = listItem!!.driver!!.javaClass.simpleName.replace("SerialDriver", "")
        else
            text1.text = listItem!!.driver!!.javaClass.simpleName.replace(
                "SerialDriver",
                ""
            ) + ", Port " + listItem.port

        text2.setText(String.format(Locale.US, "Vendor %04X, Product %04X", listItem.usbdevice.getVendorId(), listItem.usbdevice.getProductId()))
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItem = items.get(position)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.device_list_item, parent, false)

        val item = getItem(position)

        var text1: TextView = view.findViewById(R.id.t1)
        var text2: TextView = view.findViewById(R.id.t2)

        if (listItem.driver == null)
            text1.setText("<no driver")
        else if(listItem.driver!!.ports.size == 1)
            text1.text = listItem!!.driver!!.javaClass.simpleName.replace("SerialDriver", "")
        else
            text1.text = listItem!!.driver!!.javaClass.simpleName.replace(
                "SerialDriver",
                ""
            ) + ", Port " + listItem.port

        text2.setText(String.format(Locale.US, "Vendor %04X, Product %04X", listItem.usbdevice.getVendorId(), listItem.usbdevice.getProductId()))
        return view
    }
}