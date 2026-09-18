package com.boxproxy.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.boxproxy.app.databinding.FragmentSettingsBinding
import com.boxproxy.app.model.AppSettings
import com.boxproxy.app.model.ProxyMethod
import com.boxproxy.app.model.ProxyMode
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val controller by lazy { (requireActivity() as MainActivity).proxyController }
    private val coreManager by lazy { controller.coreManagerPublic }
    private var currentSettings = AppSettings()
    private var selectedPackages: MutableList<String> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        loadSettings()
        binding.btnSelectApps.setOnClickListener {
            AppSelectorDialog.newInstance(selectedPackages) { packages ->
                selectedPackages = packages.toMutableList()
                updateSelectedAppsText()
            }.show(parentFragmentManager, "app_selector")
        }
        binding.btnSave.setOnClickListener { saveSettings() }
    }

    private fun setupSpinners() {
        binding.spinnerProxyMethod.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            ProxyMethod.values().map { it.name }
        )
        binding.spinnerProxyMode.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            ProxyMode.values().map { it.name }
        )
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            currentSettings = controller.getSettings()
            selectedPackages = currentSettings.userPackages.toMutableList()
            val cores = coreManager.listCores().map { it.fileName }
            binding.spinnerCore.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item,
                cores.ifEmpty { listOf("(upload core first)") }
            )
            val coreIndex = cores.indexOf(currentSettings.selectedCore)
            if (coreIndex >= 0) binding.spinnerCore.setSelection(coreIndex)
            val configs = coreManager.listConfigs().map { it.fileName }
            binding.spinnerConfig.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item,
                configs.ifEmpty { listOf("(upload config first)") }
            )
            val configIndex = configs.indexOf(currentSettings.selectedConfig)
            if (configIndex >= 0) binding.spinnerConfig.setSelection(configIndex)
            binding.etStartCommand.setText(currentSettings.startCommand)
            binding.spinnerProxyMethod.setSelection(
                ProxyMethod.values().indexOf(currentSettings.proxyMethod).coerceAtLeast(0)
            )
            binding.spinnerProxyMode.setSelection(
                ProxyMode.values().indexOf(currentSettings.proxyMode).coerceAtLeast(0)
            )
            binding.switchIpv6.isChecked = currentSettings.ipv6Enable
            binding.switchAutoStart.isChecked = currentSettings.autoStart
            binding.etTproxyPort.setText(currentSettings.tproxyPort.toString())
            binding.etRedirPort.setText(currentSettings.redirPort.toString())
            binding.etApList.setText(currentSettings.apList.joinToString(" "))
            binding.etIgnoreOut.setText(currentSettings.ignoreOutList.joinToString(" "))
            binding.etGidList.setText(currentSettings.gidList.joinToString(" "))
            updateSelectedAppsText()
        }
    }

    private fun updateSelectedAppsText() {
        binding.tvSelectedApps.text = "Selected ${selectedPackages.size} apps"
    }

    private fun saveSettings() {
        lifecycleScope.launch {
            val cores = coreManager.listCores().map { it.fileName }
            val configs = coreManager.listConfigs().map { it.fileName }
            val selectedCore = if (cores.isNotEmpty()) cores.getOrElse(binding.spinnerCore.selectedItemPosition) { "" } else ""
            val selectedConfig = if (configs.isNotEmpty()) configs.getOrElse(binding.spinnerConfig.selectedItemPosition) { "" } else ""
            val apList = binding.etApList.text?.toString()?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            val ignoreOut = binding.etIgnoreOut.text?.toString()?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            val gidList = binding.etGidList.text?.toString()?.trim()?.split("\\s+".toRegex())?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
            val newSettings = currentSettings.copy(
                selectedCore = selectedCore,
                selectedConfig = selectedConfig,
                startCommand = binding.etStartCommand.text?.toString()?.trim() ?: "",
                proxyMethod = ProxyMethod.values()[binding.spinnerProxyMethod.selectedItemPosition.coerceIn(0, ProxyMethod.values().lastIndex)],
                proxyMode = ProxyMode.values()[binding.spinnerProxyMode.selectedItemPosition.coerceIn(0, ProxyMode.values().lastIndex)],
                userPackages = selectedPackages,
                gidList = gidList,
                apList = apList,
                ignoreOutList = ignoreOut,
                ipv6Enable = binding.switchIpv6.isChecked,
                autoStart = binding.switchAutoStart.isChecked,
                tproxyPort = binding.etTproxyPort.text?.toString()?.toIntOrNull() ?: currentSettings.tproxyPort,
                redirPort = binding.etRedirPort.text?.toString()?.toIntOrNull() ?: currentSettings.redirPort
            )
            controller.saveSettings(newSettings)
            currentSettings = newSettings
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
