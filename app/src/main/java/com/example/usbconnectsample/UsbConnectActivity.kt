package com.example.usbconnectsample

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_usb_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Usb设备连接页面
 */
class UsbConnectActivity : AppCompatActivity() {

    //请求权限
    private val mPermissionIntent by lazy {
        PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
    }

    //usb设备列表适配器
    private val mAdapter by lazy {
        object : BaseQuickAdapter<UsbDevice, BaseViewHolder>(
            android.R.layout.simple_list_item_2, null
        ) {
            override fun convert(holder: BaseViewHolder, item: UsbDevice) {
                holder.setText(android.R.id.text1, item.deviceName)
                holder.setText(
                    android.R.id.text2, "deviceId:${item.deviceId}  vendorId:${item.vendorId}"
                )
            }
        }
    }

    //加载弹窗
    private val mLoadingDialog by lazy { LoadingDialog.build(this) }

    companion object {
        //Usb打印广播
        private const val ACTION_USB_PERMISSION = "com.production.print.usb.action"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_connect)

        initView()
        initListener()
        getUsbDevices()
    }

    override fun onResume() {
        super.onResume()
        //注册插拔广播
        val mUsbFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mUsbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(mUsbReceiver, mUsbFilter)
        //注册usb权限广播
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(mPermissionReceiver, permissionFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbReceiver)
        unregisterReceiver(mPermissionReceiver)
    }

    private fun initView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        recyclerView.adapter = mAdapter
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun initListener() {
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            getUsbDevices()
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            checkPermission(mAdapter.data[position])
        }
    }

    /**
     * 获取usb插拔广播
     */
    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_DETACHED, UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    getUsbDevices()
                }
            }
        }
    }

    /**
     * 连接 USB 设备需要权限，先检查是否有权限
     */
    private fun checkPermission(device: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
        usbManager?.apply {
            if (hasPermission(device)) {
                connectUsb(device)
            } else {
                requestPermission(device, mPermissionIntent)
            }
        }
    }

    /**
     * 监听USB权限广播
     */
    private val mPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action
                && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            ) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.apply { connectUsb(this) }
            }
        }
    }

    /**
     * 获取 USB 设备
     */
    private fun getUsbDevices() {
        val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
        mAdapter.setNewInstance(usbManager?.deviceList?.values?.toMutableList())
    }

    private fun connectUsb(device: UsbDevice) {
        lifecycleScope.launch(Dispatchers.Main) {
            mLoadingDialog.show()
            val result = withContext(Dispatchers.IO) {
                Printer.connectUsb(device, getSystemService(Context.USB_SERVICE) as UsbManager)
            }
            mLoadingDialog.dismiss()
            if (result) {
                Toast.makeText(applicationContext, "USB设备连接成功", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(applicationContext, "USB设备连接失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}















