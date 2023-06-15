package com.example.myapplication.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R


class GantiIdFragment : DialogFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ganti_id, container, false)
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpanId)
        val edtId = view.findViewById<EditText>(R.id.edtIdPerangkat)
        btnSimpan.setOnClickListener {
            (activity as? MyDialogListener)?.onDialogPositiveClick(edtId.text.toString().trim())
            dialog?.dismiss()
        }
        return view
    }

    interface MyDialogListener {
        fun onDialogPositiveClick(inputData: String)
    }
}