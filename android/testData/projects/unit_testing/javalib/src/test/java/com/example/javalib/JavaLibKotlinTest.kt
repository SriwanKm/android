package com.example.javalib

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class JavaLibKotlinTest {
    @Test
    fun referenceJavaLibJavaClass() {
        assertEquals("JavaLibJavaClass", JavaLibJavaClass().name)
    }

    @Test
    fun referenceJavaLibKotlinClass() {
        assertEquals("JavaLibKotlinClass", JavaLibKotlinClass().name)
    }

    @Test
    fun prodJavaResourcesOnClasspath() {
        val url = javaClass.classLoader.getResource("javalib_resource_file.txt")
        assertNotNull(url)

        val stream = javaClass.classLoader.getResourceAsStream("javalib_resource_file.txt")
        assertNotNull(stream)
        val s = String(stream.readBytes(), StandardCharsets.UTF_8).trim()
        assertEquals("javalib", s)
    }

    @Test
    fun javaResourcesOnClasspath() {
        val url = javaClass.classLoader.getResource("javalib_test_resource_file.txt")
        assertNotNull(url)

        val stream = javaClass.classLoader.getResourceAsStream("javalib_test_resource_file.txt")
        assertNotNull(stream)
        val s = String(stream.readBytes(), StandardCharsets.UTF_8).trim()
        assertEquals("javalib test", s)
    }

    @Test
    fun workingDir() {
        assertTrue(File("").absolutePath.endsWith("javalib"))
    }

    @Test
    fun assertions() {
        try {
            assert(false)
            fail("assertions disabled");
        } catch (e: AssertionError) {
            // expected
        }
    }
}
