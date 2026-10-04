package com.safehome.app.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.safehome.app.R
import com.safehome.app.databinding.SheetFacilityInfoBinding

class FacilityInfoSheet : BottomSheetDialogFragment() {

    private var _binding: SheetFacilityInfoBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TYPE = "arg_type"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ADDRESS = "arg_address"
        private const val ARG_ACTIVE = "arg_active"

        fun newInstance(type: String, title: String, address: String?, isActive: Boolean) =
            FacilityInfoSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                    putString(ARG_TITLE, title)
                    putString(ARG_ADDRESS, address)
                    putBoolean(ARG_ACTIVE, isActive)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetFacilityInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString(ARG_TYPE) ?: "CCTV"
        val isActive = arguments?.getBoolean(ARG_ACTIVE) ?: true

        binding.ivFacilityIcon.setImageResource(iconFor(type))
        binding.tvFacilityTitle.text = arguments?.getString(ARG_TITLE) ?: ""
        binding.tvFacilityAddress.text =
            arguments?.getString(ARG_ADDRESS)?.takeIf { it.isNotBlank() } ?: "주소 정보 없음"

        if (isActive) {
            binding.tvStatus.text = "정상 운영 중"
            binding.statusDot.setBackgroundResource(R.drawable.dot_status_active)
        } else {
            binding.tvStatus.text = "현재 미작동"
            binding.statusDot.setBackgroundResource(R.drawable.dot_status_inactive)
        }
        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun iconFor(type: String): Int = when (type) {
        "CCTV" -> R.drawable.ic_marker_cctv_default
        "EMERGENCY_BELL" -> R.drawable.ic_marker_emergency_bell_default
        "POLICE" -> R.drawable.ic_marker_police_default
        else -> R.drawable.ic_marker_cctv_default
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}