package com.momenta.translator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.momenta.translator.data.RecentTranslation
import com.momenta.translator.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<RecentTranslation, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentTranslation) {
            binding.tvOriginal.text = item.original
            binding.tvTranslated.text = item.translated
            binding.tvTime.text = formatTime(item.timestamp)
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "刚刚"
                diff < 3600_000 -> "${diff / 60_000}分钟前"
                diff < 86400_000 -> "${diff / 3600_000}小时前"
                diff < 604800_000 -> "${diff / 86400_000}天前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecentTranslation>() {
        override fun areItemsTheSame(
            oldItem: RecentTranslation,
            newItem: RecentTranslation
        ): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(
            oldItem: RecentTranslation,
            newItem: RecentTranslation
        ): Boolean {
            return oldItem == newItem
        }
    }
}
