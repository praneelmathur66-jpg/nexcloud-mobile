package com.example

import com.example.data.repository.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class NexCloudLogicTest {

    @Test
    fun formatBytes_formatsProperly() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("1 KB", formatBytes(1024L))
        assertEquals("1.5 MB", formatBytes(1572864L))
        assertEquals("1 GB", formatBytes(1073741824L))
    }

    @Test
    fun cleanPart_sanitizesFilenames() {
        fun cleanPart(name: String): String {
            return name
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                .replace(Regex("^\\.+$"), "_")
                .trim()
        }

        assertEquals("my_folder", cleanPart("my:folder"))
        assertEquals("document_2026.pdf", cleanPart("document?2026.pdf"))
        assertEquals("_", cleanPart("..."))
        assertEquals("clean_name", cleanPart("clean_name"))
    }
}
