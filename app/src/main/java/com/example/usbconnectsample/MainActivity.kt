package com.example.usbconnectsample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 主页面
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //跳转到蓝牙连接页面
        connectUsb.setOnClickListener {
            startActivity(Intent(this, UsbConnectActivity::class.java))
        }
        //测试打印二维码
        tvPrintQrCodeTest.setOnClickListener {
            printQRCodeTest()
        }
        //测试打印字符串
        tvPrintStringTest.setOnClickListener {
            printStringTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Printer.closeConnection()
    }

    /**
     * 发送的协议和字节码是基于 汉印HM-A300 打印设备的
     * 打印二维码，使用协程放到子线程中操作，同时绑定生命周期
     */
    private fun printQRCodeTest() {
        lifecycleScope.launch(Dispatchers.IO) {
            Printer.openEndStatic(true)
            Printer.printAreaSize(0, 200, 200, 300, 1)
            Printer.printQR("BARCODE", 0, 0, 2, 10, "https://www.baidu.com")
            Printer.print()
            showPrintStatus()
        }
    }

    /**
     * 发送的协议和字节码是基于 汉印HM-A300 打印设备的
     * 打印字符串，使用协程放到子线程中操作，同时绑定生命周期
     */
    private fun printStringTest() {
        lifecycleScope.launch(Dispatchers.IO) {
            Printer.openEndStatic(true)
            Printer.printAreaSize(0, 200, 200, 120, 1)
            Printer.align("CENTER")
            Printer.text("T", 8, 20, 0, "CODE128fasdf232342dafasdfasdf2342343")
            Printer.print()
            showPrintStatus()
        }
    }

    /**
     * 发送的协议和字节码是基于 汉印HM-A300 打印设备的
     * 获取打印结果，使用协程放到子线程中操作，同时绑定生命周期
     */
    private fun showPrintStatus() {
        val resultStatus = Printer.getEndStatus(10)
        Printer.openEndStatic(false)
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                "result：${if (resultStatus == 0) "success" else "fail$resultStatus"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

}