package com.example.iptvmate

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

object XmltvParser {

    private const val TAG = "XmltvParser"

    fun parse(inputStream: InputStream): Map<String, EpgChannelData> {
        val channels = mutableMapOf<String, EpgChannelData>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(inputStream)
            doc.documentElement.normalize()

            val channelNodes = doc.getElementsByTagName("channel")
            for (i in 0 until channelNodes.length) {
                val node = channelNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val id = element.getAttribute("id")
                    if (!id.isNullOrBlank()) {
                        var displayName: String? = null
                        val nameNodes = element.getElementsByTagName("display-name")
                        if (nameNodes.length > 0) {
                            displayName = nameNodes.item(0).textContent?.trim()
                        }
                        channels[id] = EpgChannelData(id, displayName)
                    }
                }
            }

            val progNodes = doc.getElementsByTagName("programme")
            for (i in 0 until progNodes.length) {
                val node = progNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val channelId = element.getAttribute("channel")
                    val startStr = element.getAttribute("start")
                    val stopStr = element.getAttribute("stop")
                    val startTime = parseXmltvTime(startStr)
                    val stopTime = parseXmltvTime(stopStr)

                    var title = ""
                    val titleNodes = element.getElementsByTagName("title")
                    if (titleNodes.length > 0) {
                        title = titleNodes.item(0).textContent?.trim() ?: ""
                    }

                    if (!channelId.isNullOrBlank() && title.isNotEmpty() && startTime > 0) {
                        val program = EpgProgram(
                            channelId = channelId,
                            title = title,
                            startTime = startTime,
                            stopTime = stopTime
                        )
                        channels.getOrPut(channelId) {
                            EpgChannelData(channelId, null)
                        }.programs.add(program)
                    }
                }
            }
        } catch (e: Exception) {
            logError(TAG, "Error parsing XMLTV EPG: ${e.message}", e)
        }
        return channels
    }

    fun parseXmltvTime(timeStr: String?): Long {
        if (timeStr.isNullOrBlank()) return 0L
        val clean = timeStr.trim()
        val formats = arrayOf(
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        )
        for (fmt in formats) {
            try {
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                val date = fmt.parse(clean.take(14) + if (clean.length > 14) clean.drop(14) else "")
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        try {
            val digits = clean.filter { it.isDigit() }
            if (digits.length >= 12) {
                val year = digits.substring(0, 4).toInt()
                val month = digits.substring(4, 6).toInt() - 1
                val day = digits.substring(6, 8).toInt()
                val hour = digits.substring(8, 10).toInt()
                val minute = digits.substring(10, 12).toInt()
                val second = if (digits.length >= 14) digits.substring(12, 14).toInt() else 0
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(year, month, day, hour, minute, second)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        } catch (_: Exception) {
            logWarn(TAG, "Failed to parse timeStr: $timeStr")
        }
        return 0L
    }

    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (e: Throwable) {
            println("[$tag] $message: ${throwable?.message}")
        }
    }

    private fun logWarn(tag: String, message: String) {
        try {
            Log.w(tag, message)
        } catch (e: Throwable) {
            println("[$tag] $message")
        }
    }
}
