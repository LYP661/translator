package com.momenta.translator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.momenta.translator.data.TranslationHistoryManager
import com.momenta.translator.databinding.FragmentHistoryBinding
import com.momenta.translator.ui.adapter.HistoryAdapter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyManager: TranslationHistoryManager
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyManager = TranslationHistoryManager(requireContext())

        // 设置 RecyclerView
        adapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        // 清空按钮
        binding.btnClear.setOnClickListener {
            historyManager.clearHistory()
            refreshHistory()
        }

        // 加载历史
        refreshHistory()
    }

    override fun onResume() {
        super.onResume()
        // 每次显示时刷新历史
        refreshHistory()
    }

    private fun refreshHistory() {
        val history = historyManager.getHistory()

        if (history.isEmpty()) {
            binding.rvHistory.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.btnClear.visibility = View.GONE
        } else {
            binding.rvHistory.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.btnClear.visibility = View.VISIBLE
            adapter.submitList(history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
