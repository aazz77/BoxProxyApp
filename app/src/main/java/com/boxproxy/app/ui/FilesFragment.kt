package com.boxproxy.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.boxproxy.app.databinding.FragmentFilesBinding
import kotlinx.coroutines.launch

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private val controller by lazy { (requireActivity() as MainActivity).proxyController }
    private val coreManager by lazy { controller.coreManagerPublic }
    private var uploadType = UploadType.CORE

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handlePickedFile(uri) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnUploadCore.setOnClickListener {
            uploadType = UploadType.CORE
            openFilePicker()
        }
        binding.btnUploadConfig.setOnClickListener {
            uploadType = UploadType.CONFIG
            openFilePicker()
        }
        binding.rvCores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConfigs.layoutManager = LinearLayoutManager(requireContext())
        refreshLists()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        pickFileLauncher.launch(intent)
    }

    private fun handlePickedFile(uri: Uri) {
        var fileName = "unknown"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        lifecycleScope.launch {
            when (uploadType) {
                UploadType.CORE -> {
                    val result = coreManager.importCore(uri, fileName)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Core uploaded: $fileName", Toast.LENGTH_SHORT).show()
                        refreshLists()
                    } else {
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_LONG).show()
                    }
                }
                UploadType.CONFIG -> {
                    val result = coreManager.importConfig(uri, fileName)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Config uploaded: $fileName", Toast.LENGTH_SHORT).show()
                        refreshLists()
                    } else {
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun refreshLists() {
        val cores = coreManager.listCores()
        val configs = coreManager.listConfigs()
        binding.tvCoreEmpty.visibility = if (cores.isEmpty()) View.VISIBLE else View.GONE
        binding.tvConfigEmpty.visibility = if (configs.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCores.adapter = SimpleFileAdapter(cores.map { it.fileName to formatSize(it.size) }) { name ->
            lifecycleScope.launch {
                coreManager.deleteCore(name)
                refreshLists()
            }
        }
        binding.rvConfigs.adapter = SimpleFileAdapter(configs.map { it.fileName to formatSize(it.size) }) { name ->
            lifecycleScope.launch {
                coreManager.deleteConfig(name)
                refreshLists()
            }
        }
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "%.1f MB".format(size / (1024.0 * 1024))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLists()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class UploadType { CORE, CONFIG }
}

class SimpleFileAdapter(
    private val items: List<Pair<String, String>>,
    private val onDelete: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SimpleFileAdapter.VH>() {

    class VH(val view: android.widget.TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = android.widget.TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 24, 32, 24)
            textSize = 15f
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (name, size) = items[position]
        holder.view.text = "$name  ($size)"
        holder.view.setOnLongClickListener {
            onDelete(name)
            true
        }
    }

    override fun getItemCount() = items.size
}
