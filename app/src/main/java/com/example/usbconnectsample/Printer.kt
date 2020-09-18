package com.example.usbconnectsample

import android.hardware.usb.*
import java.io.IOException

/**
 * 打印工具类
 */
object Printer {

    private var mDevice: UsbDevice? = null
    private var mMmConnection: UsbDeviceConnection? = null
    private var mMmItf: UsbInterface? = null
    private var mUsbEndpointIn: UsbEndpoint? = null
    private var mUsbEndpointOut: UsbEndpoint? = null

    /**
     * 连接USB设备
     */
    fun connectUsb(usbDevice: UsbDevice, usbManager: UsbManager): Boolean {
        closeConnection()
        try {
            mDevice = usbDevice
            val i = 0
            if (i < usbDevice.interfaceCount) {
                val usbInterface: UsbInterface? = usbDevice.getInterface(i)
                if (usbInterface != null) mMmItf = usbInterface
            }

            if (mMmItf == null) return false
            mMmConnection = usbManager.openDevice(usbDevice)
            if (mMmConnection == null || !mMmConnection!!.claimInterface(mMmItf, true)) return false
            for (n in 0 until mMmItf!!.endpointCount) {
                val ep = mMmItf!!.getEndpoint(n)
                if (ep.type == 2) {
                    if (ep.direction == 0) {
                        mUsbEndpointOut = ep
                    } else {
                        mUsbEndpointIn = ep
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mUsbEndpointOut != null && mUsbEndpointIn != null
    }

    /**
     * 关闭连接
     */
    fun closeConnection() {
        try {
            mDevice = null
            mMmItf?.apply { mMmConnection?.releaseInterface(mMmItf) }
            mMmItf = null
            mMmConnection?.close()
            mMmConnection = null
            mUsbEndpointIn = null
            mUsbEndpointOut = null
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 页标签开始指令
     * @param offset 此值使所有字段将水平偏移指定的单位数量
     * @param horizontal 水平方向的 dpi。（根据打印机的 dpi 设置，200dpi 打印机：8px=1mm）
     * @param vertical 垂直方向的 dpi。（同上）
     * @param height 整个标签的高度。（单位：px）
     * @param qty 打印的次数
     */
    fun printAreaSize(offset: Int, horizontal: Int, vertical: Int, height: Int, qty: Int) {
        writeData(ByteUtils.getBytes("! $offset $horizontal $vertical $height $qty\r\n"))
    }

    /**
     * @param command 打印方向 BARCODE:水平方向  VBARCODE:垂直方向
     * @param x 二维码的起始横坐标
     * @param y 二维码的起始纵坐标
     * @param M QR 的类型：类型 1 和类型 2；类型 2 是增加了个别的符号，提供了额外的功 能
     * @param U 单位宽度/模块的单元高度。 范围是 1 到 32 默认为 6
     * @param data 二维码的数据
     */
    fun printQR(command: String, x: Int, y: Int, M: Int, U: Int, data: String) {
        writeData(ByteUtils.getBytes("$command QR $x $y M $M U $U\r\nMA,$data\r\nENDQR\r\n"))
    }

    /**
     * 开始打印
     */
    fun print() {
        writeData(ByteUtils.getBytes("PRINT\r\n"))
    }

    /**
     * 获取打印机状态
     * @param timeOut 获取状态超时时间，单位秒
     */
    fun getEndStatus(timeOut: Int): Int {
        var status = -1
        var readBytes: ByteArray? = null
        while (status == -1) {
            if (readData(timeOut).also { readBytes = it }.isEmpty()) {
                return -1
            }
            val readStr: String = byteToHex(readBytes)
            status = readStr.lastIndexOf("CC")
        }
        return readBytes!![status / 3 + 1].toInt()
    }

    /**
     * 设置字体对齐方式
     * @param align CENTER:居中   LEFT:居左     RIGHT:居右
     */
    fun align(align: String) {
        writeData(ByteUtils.getBytes("$align\r\n"))
    }

    /**
     * 打印文本
     * @param command 文字的方向 T：水平。 T90：逆时针旋转 90 度。 T180：逆时针旋转 180 度。 T270：逆时针旋转 270 度。
     * @param font 字体。
     *             0：24x24 或 12x24，视中英文而定。（泰语：24x48）
     *             1：7x19（英文），24x24（繁体）。
     *             3：20x20 或 10x20，视中英文而定。
     *             4：32x32 或 16x32，由 ID3 字体宽高各放大 2 倍。
     *             8：24x24 或 12x24，视中英文而定。
     *             55：16x16 或 8x16，视中英文而定。
     * @param x 起始点的横坐标。（单位：px）
     * @param y 起始点的纵坐标。（单位：px）
     * @param data 文本数据。
     */
    fun text(command: String, font: Int, x: Int, y: Int, data: String) {
        writeData(ByteUtils.getBytes("$command $font 0 $x $y $data\r\n"))
    }

    /**
     * 获取打印完成时状态开关
     */
    fun openEndStatic(canGetStatus: Boolean) {
        if (canGetStatus) {
            writeData(byteArrayOf(27, 27, 49, 1))
        } else {
            writeData(byteArrayOf(27, 27, 49, 0))
        }
    }

    /**
     * 读取数据
     * @param timeOut 获取数据超时时间，单位 秒
     */
    private fun readData(timeOut: Int): ByteArray {
        val retArr = ByteArray(100)
        if (mUsbEndpointIn == null) return retArr
        try {
            mMmConnection!!.bulkTransfer(mUsbEndpointIn!!, retArr, retArr.size, timeOut * 1000)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return retArr
    }

    /**
     * 写入数据
     */
    private fun writeData(writeBytes: ByteArray) {
        if (mMmConnection == null || mUsbEndpointOut == null) return
        try {
            val writeByteLength = writeBytes.size
            val tempBytes = ByteArray(10000)
            val needWriteTimes = writeByteLength / 10000
            var tempIndex: Int
            for (i in 0 until needWriteTimes) {
                tempIndex = i * 10000
                while (tempIndex < (i + 1) * 10000) {
                    tempBytes[tempIndex % 10000] = writeBytes[tempIndex]
                    ++tempIndex
                }
                mMmConnection!!.bulkTransfer(mUsbEndpointOut!!, tempBytes, tempBytes.size, 1000)
            }
            if (writeByteLength % 10000 != 0) {
                val endIndex = ByteArray(writeBytes.size - needWriteTimes * 10000)
                tempIndex = needWriteTimes * 10000
                while (tempIndex < writeBytes.size) {
                    endIndex[tempIndex - needWriteTimes * 10000] = writeBytes[tempIndex]
                    ++tempIndex
                }
                mMmConnection!!.bulkTransfer(mUsbEndpointOut!!, endIndex, endIndex.size, 1000)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun byteToHex(data: ByteArray?): String {
        if (data == null) return ""
        val retArr = StringBuilder(data.size)
        for (element in data) {
            retArr.append(String.format("%02X ", element))
        }
        return retArr.toString()
    }

}



























