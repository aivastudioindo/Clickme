package com.clickme.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clickme.app.databinding.ItemNotificationBinding
import com.clickme.app.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View

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

        fun bind(item: NotificationItem) {
            b.pkgName.text = item.appName
            b.timestamp.text = fmt.format(Date(item.timestamp))
            b.title.text = item.title.ifEmpty { "—" }
            val body = item.text.ifEmpty { item.bigText }
            b.body.text = body.ifEmpty { "—" }
            b.newDot.visibility =
                if (item.isNew) View.VISIBLE else View.GONE
        }
    }
}
