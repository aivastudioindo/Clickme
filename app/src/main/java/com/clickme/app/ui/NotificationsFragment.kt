package com.clickme.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.clickme.app.databinding.FragmentNotificationsBinding
import com.clickme.app.repo.NotificationRepository
import com.clickme.app.NotificationAdapter

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = context ?: return

        adapter = NotificationAdapter()
        binding.list.layoutManager = LinearLayoutManager(ctx)
        binding.list.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            // catch-up: minta ulang notifikasi aktif dari sistem
            binding.swipeRefresh.isRefreshing = false
            NotificationRepository.markAllRead()
        }

        NotificationRepository.observe { items ->
            binding.emptyState.visibility =
                if (items.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
