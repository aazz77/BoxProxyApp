package com.boxproxy.app.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Filter
import android.widget.Filterable
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boxproxy.app.databinding.DialogAppSelectorBinding
import com.boxproxy.app.databinding.ItemAppBinding
import com.boxproxy.app.manager.IptablesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectorDialog : DialogFragment() {

    private var _binding: DialogAppSelectorBinding? = null
    private val binding get() = _binding!!
    private var allApps: List<IptablesManager.AppInfo> = emptyList()
    private var selectedPackages: MutableSet<String> = mutableSetOf()
    private var onConfirm: ((List<String>) -> Unit)? = null
    private lateinit var adapter: AppAdapter

    companion object {
        fun newInstance(preSelected: List<String>, onConfirm: (List<String>) -> Unit): AppSelectorDialog {
            return AppSelectorDialog().apply {
                this.selectedPackages = preSelected.toMutableSet()
                this.onConfirm = onConfirm
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAppSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        adapter = AppAdapter(selectedPackages) { updateCount() }
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s?.toString() ?: "")
            }
        })
        binding.switchShowSystem.setOnCheckedChangeListener { _, isChecked ->
            loadApps(includeSystem = isChecked)
        }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            onConfirm?.invoke(selectedPackages.toList())
            dismiss()
        }
        loadApps(includeSystem = false)
        updateCount()
    }

    private fun loadApps(includeSystem: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            val apps = withContext(Dispatchers.IO) {
                val controller = (requireActivity() as MainActivity).proxyController
                controller.iptablesManagerPublic.getInstalledApps(includeSystem)
            }
            allApps = apps
            adapter.submitList(apps)
        }
    }

    private fun updateCount() {
        binding.tvSelectedCount.text = "Selected ${selectedPackages.size} apps"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AppAdapter(
        private val selected: MutableSet<String>,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>(), Filterable {

        private var displayList: List<IptablesManager.AppInfo> = emptyList()
        private var fullList: List<IptablesManager.AppInfo> = emptyList()

        fun submitList(list: List<IptablesManager.AppInfo>) {
            fullList = list
            displayList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(displayList[position])
        }

        override fun getItemCount() = displayList.size

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) fullList else fullList.filter {
                    it.label.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query) ||
                        it.uid.toString().contains(query)
                }
                return FilterResults().apply { values = filtered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                displayList = (results?.values as? List<IptablesManager.AppInfo>) ?: emptyList()
                notifyDataSetChanged()
            }
        }

        inner class VH(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(app: IptablesManager.AppInfo) {
                binding.tvLabel.text = app.label
                binding.tvPackage.text = app.packageName
                binding.tvUid.text = "UID: ${app.uid}" + if (app.isSystem) " (system)" else ""
                binding.cbSelect.setOnCheckedChangeListener(null)
                binding.cbSelect.isChecked = selected.contains(app.packageName)
                binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selected.add(app.packageName) else selected.remove(app.packageName)
                    onSelectionChanged()
                }
                binding.root.setOnClickListener {
                    binding.cbSelect.isChecked = !binding.cbSelect.isChecked
                }
            }
        }
    }
}
