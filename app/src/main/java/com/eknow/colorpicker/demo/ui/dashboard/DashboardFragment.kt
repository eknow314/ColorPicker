package com.eknow.colorpicker.demo.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.eknow.colorpicker.ColorBean
import com.eknow.colorpicker.demo.R
import com.eknow.colorpicker.demo.databinding.FragmentDashboardBinding
import com.eknow.colorpicker.hsv.HSVColorPickerView

@SuppressLint("SetTextI18n")
class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    private var hsColorPickerView: HSVColorPickerView? = null
    private var vColorPickerView: HSVColorPickerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        hsColorPickerView = binding.hsColorPickerView
        vColorPickerView = binding.vColorPickerView
        val tvHsvColor = binding.tvHsvColor
        val tvRgbColor = binding.tvRgbColor

        hsColorPickerView?.setOnColorChangedListener(object :
            HSVColorPickerView.OnColorChangedListener {
            override fun onColorChanged(pickerView: HSVColorPickerView, color: Int) {
                vColorPickerView?.setColor(color)
            }
        })

        vColorPickerView?.setOnColorChangedListener(object :
            HSVColorPickerView.OnColorChangedListener {
            override fun onColorChanged(pickerView: HSVColorPickerView, color: Int) {
                tvHsvColor.setBackgroundColor(color)
                tvRgbColor.setBackgroundColor(color)

                val colorBean = ColorBean(color)
                tvHsvColor.text =
                    "h:${colorBean.hsv[0]}  s:${colorBean.hsv[1]}  v:${colorBean.hsv[2]}"
                tvRgbColor.text =
                    "r:${colorBean.r}  g:${colorBean.g}  b:${colorBean.b}  rgb:${colorBean.rgbStr()}"

                if (colorBean.hsv[2] < 0.5f) {
                    tvHsvColor.setTextColor(Color.WHITE)
                    tvRgbColor.setTextColor(Color.WHITE)
                } else {
                    tvHsvColor.setTextColor(Color.BLACK)
                    tvRgbColor.setTextColor(Color.BLACK)
                }
            }
        })

        hsColorPickerView?.setColor(ColorBean(180f, 0.5f, 0.5f).rgb)

        binding.toggleEnable.setOnCheckedChangeListener { _, b ->
            hsColorPickerView?.setEnable(b)
            vColorPickerView?.setEnable(b)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hsColorPickerView?.recycle()
        hsColorPickerView = null
        vColorPickerView?.recycle()
        vColorPickerView = null
        _binding = null
    }
}