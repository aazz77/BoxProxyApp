package com.boxproxy.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.boxproxy.app.databinding.FragmentHomeBinding
import com.boxproxy.app.model.ProxyState
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val controller by lazy { (requireActivity() as MainActivity).proxyController }

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
        setupButtons()
        observeStatus()
        refreshUI()
        checkRoot()
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            lifecycleScope.launch {
                binding.btnStart.isEnabled = false
                val result = controller.start()
                binding.btnStart.isEnabled = true
                if (result.isFailure) {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: "Start failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.btnStop.setOnClickListener {
            lifecycleScope.launch { controller.stop() }
        }
        binding.btnRestart.setOnClickListener {
            lifecycleScope.launch {
                val result = controller.restart()
                if (result.isFailure) {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: "Restart failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.btnEmergency.setOnClickListener {
            lifecycleScope.launch {
                controller.emergencyStop()
                Toast.makeText(requireContext(), "Emergency stop done", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            controller.status.collectLatest { status ->
                updateStatusUI(status.state, status.message, status.pid)
            }
        }
    }

    private fun updateStatusUI(state: ProxyState, message: String, pid: Int) {
        val (text, color) = when (state) {
            ProxyState.STOPPED -> "Stopped" to 0xFF757575.toInt()
            ProxyState.STARTING -> "Starting..." to 0xFFF57C00.toInt()
            ProxyState.RUNNING -> "Running" to 0xFF388E3C.toInt()
            ProxyState.STOPPING -> "Stopping..." to 0xFFF57C00.toInt()
            ProxyState.ERROR -> "Error" to 0xFFD32F2F.toInt()
        }
        binding.tvStatus.text = text
        binding.tvStatus.setTextColor(color)
        binding.tvStatusDetail.text = message
    }

    private fun refreshUI() {
        lifecycleScope.launch {
            val settings = controller.getSettings()
            binding.tvCurrentCore.text = settings.selectedCore.ifBlank { "Not selected" }
            binding.tvCurrentConfig.text = settings.selectedConfig.ifBlank { "Not selected" }
            binding.tvStartCommand.text = settings.startCommand.ifBlank { "Not set" }
            controller.refreshStatus()
        }
    }

    private fun checkRoot() {
        Shell.getShell { shell ->
            activity?.runOnUiThread {
                binding.tvRootStatus.text = if (shell.isRoot) {
                    "Root granted"
                } else {
                    "Root required"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
