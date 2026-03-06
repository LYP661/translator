package com.momenta.translator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.momenta.translator.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 拍照翻译
        binding.cardCamera.setOnClickListener {
            navigateToFragment(CameraFragment())
        }

        // 文字翻译
        binding.cardText.setOnClickListener {
            navigateToFragment(TextInputFragment())
        }

        // 语音翻译
        binding.cardVoice.setOnClickListener {
            navigateToFragment(VoiceFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace((view?.parent as? ViewGroup)?.id ?: android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
