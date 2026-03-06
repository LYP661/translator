package com.momenta.translator.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.momenta.translator.databinding.FragmentSettingsBinding
import com.momenta.translator.viewmodel.TranslateViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranslateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVersionInfo()
        setupListeners()
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            binding.tvVersion.text = "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "v1.3"
        }
    }

    private fun setupListeners() {
        binding.btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空翻译历史")
            .setMessage("确定要清空所有翻译历史记录吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.clearHistory()
                showSuccessMessage()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSuccessMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle("✓ 已清空")
            .setMessage("翻译历史记录已成功清空！")
            .setPositiveButton("好的", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
