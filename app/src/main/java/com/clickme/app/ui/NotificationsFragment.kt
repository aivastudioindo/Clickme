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
import com.clickme.app.NotificationService

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter
    private val observer: (List<com.clickme.app.model.NotificationItem>) -> Unit = { items ->
        val b = _binding ?: return@observer
        b.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(items)
    }

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
            // Catch-up: ambil ulang notifikasi yang masih aktif di sistem,
            // lalu refresh tampilan. Tidak menghapus data yang sudah ada.
            NotificationService.requestActiveNotifications(requireContext())
            binding.swipeRefresh.isRefreshing = false
            NotificationRepository.observe(observer)
        }

        NotificationRepository.observe(observer)
    }

    override fun onDestroyView() {
        NotificationRepository.removeObserver(observer)
        _binding = null
        super.onDestroyView()
    }
}
