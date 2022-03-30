package com.eknow.colorpicker.demo.ui.notifications

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.eknow.colorpicker.cw.CWColorPickerView
import com.eknow.colorpicker.demo.databinding.FragmentNotificationsBinding

@SuppressLint("SetTextI18n")
class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var cwPickerView: CWColorPickerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        cwPickerView = binding.cwColorPickerView
        val tvCwColor = binding.tvCwColor

        cwPickerView?.setOnColorChangedListener(object : CWColorPickerView.OnColorChangedListener {
            override fun onColorChanged(cw: Int, color: Int) {
                tvCwColor.setBackgroundColor(color)
                val rbgStr = String.format("#%06X", 0xFFFFFF and color)
                tvCwColor.text = "cw:$cw  $rbgStr"
            }
        })

        binding.toggleEnable.setOnCheckedChangeListener { _, b ->
            cwPickerView?.setEnable(b)
        }

        cwPickerView?.setColorCW(20)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cwPickerView?.recycle()
        cwPickerView = null
        _binding = null
    }
}