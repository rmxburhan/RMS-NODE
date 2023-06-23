package com.example.myapplication.ui

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ShareActionProvider
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R


class KalibrasiVariableFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    private lateinit var sharedPReferences : SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_kalibrasi_variable, container, false)
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpan)
        val edtKalibrasi = view.findViewById<EditText>(R.id.edtKonstantaKalibrasi)
        val edtKalibrasiGo = view.findViewById<EditText>(R.id.edtKonstantaKalibrasiJarakGo)
        sharedPReferences = activity?.getSharedPreferences("kalibrasi", MODE_PRIVATE)!!
        edtKalibrasi.setText(sharedPReferences.getInt("kalibtasi", 0).toString())
        edtKalibrasiGo.setText(sharedPReferences.getInt("kalibrasiGo",0).toString())
        btnSimpan.setOnClickListener {
            var kalibrasi : Int = 0
            var kalibrasiGO : Int = 0
            if (!edtKalibrasi.text.toString().trim().isNullOrEmpty() ) {
                kalibrasi = edtKalibrasi.text.toString().toInt()
            }
            if (!edtKalibrasiGo.text.toString().trim().isNullOrEmpty()) {
                kalibrasiGO = edtKalibrasiGo.text.toString().toInt()
            }
            sharedPReferences.edit()
                .putInt("kalibrasi", kalibrasi)
                .putInt("kalibrasiGo", kalibrasiGO)
                .commit()

            dialog?.dismiss()
        }
        return view
    }
}