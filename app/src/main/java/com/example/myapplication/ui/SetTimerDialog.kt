package com.example.myapplication.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R

class SetTimerDialog : DialogFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_set_timer_dialog, container, false)
        val btnSImpan = view.findViewById<Button>(R.id.btnSimpanTimer)
        val edtInput = view.findViewById<EditText>(R.id.edtIdPerangkat)
        btnSImpan.setOnClickListener {
            if (edtInput.text.toString().isNullOrEmpty()) {
                Toast.makeText(activity, "tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            (activity as SetTimerListener).onSaveTimer(edtInput.text.toString())
            dialog?.dismiss()
        }
        return view
    }

    interface SetTimerListener {
        fun onSaveTimer(inputData : String);
    }
}