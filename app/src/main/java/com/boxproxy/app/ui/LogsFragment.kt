package com.boxproxy.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boxproxy.app.databinding.FragmentLogsBinding

class LogsFragment : Fragment() {
    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val controller by lazy { (requireActivity() as MainActivity).proxyController }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRefreshLog.setOnClickListener { loadLogs() }
        binding.btnClearLog.setOnClickListener { binding.tvLogs.text = "" }
        loadLogs()
    }

    private fun loadLogs() {
        val runLog = controller.getRunLog(400)
        val errLog = controller.getErrorLog(100)
        val combined = buildString {
            if (runLog.isNotBlank()) {
                appendLine("===== RUN LOG =====")
                appendLine(runLog)
            }
            if (errLog.isNotBlank()) {
                appendLine()
                appendLine("===== ERROR LOG =====")
                appendLine(errLog)
            }
            if (runLog.isBlank() && errLog.isBlank()) {
                append("No logs yet")
            }
        }
        binding.tvLogs.text = combined
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
