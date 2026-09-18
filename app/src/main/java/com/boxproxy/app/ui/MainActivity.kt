package com.boxproxy.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.boxproxy.app.R
import com.boxproxy.app.databinding.ActivityMainBinding
import com.boxproxy.app.manager.ProxyController
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var proxyController: ProxyController
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        proxyController = ProxyController(applicationContext)
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                Toast.makeText(this, R.string.need_root, Toast.LENGTH_LONG).show()
            }
        }
        setupBottomNav()
        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }
    }
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_files -> switchFragment(FilesFragment())
                R.id.nav_settings -> switchFragment(SettingsFragment())
                R.id.nav_logs -> switchFragment(LogsFragment())
            }
            true
        }
    }
    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
