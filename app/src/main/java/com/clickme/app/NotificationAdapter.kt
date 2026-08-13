package com.clickme.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clickme.app.databinding.ItemNotificationBinding
import com.clickme.app.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter :
    ListAdapter<NotificationItem, NotificationAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationItem>() {
            override fun areItemsTheSame(a: NotificationItem, b: NotificationItem) =
                a.id == b.id

            override fun areContentsTheSame(a: NotificationItem, b: NotificationItem) =
                a == b
        }

        private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val b: ItemNotificationBinding) :
        RecyclerView.ViewHolder(b.root) {

        private var expanded = false

        fun bind(item: NotificationItem) {
            b.pkgName.text = item.appName
            b.timestamp.text = fmt.format(Date(item.timestamp))
            b.title.text = item.title.ifEmpty { "—" }

            val hasLines = item.lines.isNotEmpty()
            if (hasLines) {
                // Notifikasi bertumpuk: baris pertama sebagai ringkasan,
                // sisanya bisa di-expand lewat tap.
                b.body.text = item.lines.first()
                b.countBadge.visibility = View.VISIBLE
                b.countBadge.text = item.lines.size.toString()
            } else {
                val body = item.text.ifEmpty { item.bigText }
                b.body.text = body.ifEmpty { "—" }
                b.countBadge.visibility = View.GONE
            }

            b.newDot.visibility =
                if (item.isNew) View.VISIBLE else View.GONE

            // Expand/collapse daftar pesan saat card di-tap
            b.linesExpanded.visibility = if (expanded && hasLines) View.VISIBLE else View.GONE
            if (hasLines) {
                b.linesExpanded.text = item.lines.joinToString("\n")
                b.root.setOnClickListener {
                    expanded = !expanded
                    b.linesExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
                }
            } else {
                b.root.setOnClickListener(null)
            }
        }
    }
}
