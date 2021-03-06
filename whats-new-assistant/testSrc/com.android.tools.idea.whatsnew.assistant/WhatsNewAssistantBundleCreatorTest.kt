/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.whatsnew.assistant

import com.android.testutils.TestUtils
import com.android.tools.idea.assistant.AssistantBundleCreator
import com.android.tools.idea.flags.StudioFlags
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.android.AndroidTestCase
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeoutException

class WhatsNewAssistantBundleCreatorTest : AndroidTestCase() {
  private lateinit var mockUrlProvider: WhatsNewAssistantURLProvider
  private lateinit var localPath: Path

  override fun setUp() {
    super.setUp()
    StudioFlags.WHATS_NEW_ASSISTANT_ENABLED.override(true)
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(true)

    // Mock url provider to simulate webserver and also class resource file
    mockUrlProvider = mock(WhatsNewAssistantURLProvider::class.java)

    val serverFile = File(myFixture.testDataPath).resolve("whatsnewassistant/server-3.3.0.xml")
    `when`(mockUrlProvider.getWebConfig(ArgumentMatchers.anyString())).thenReturn(URL("file:" + serverFile.path))

    val resourceFile = File(myFixture.testDataPath).resolve("whatsnewassistant/defaultresource-3.3.0.xml")
    `when`(mockUrlProvider.getResourceFileAsStream(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
      .thenReturn(URL("file:" + resourceFile.path).openStream())

    val tmpDir = TestUtils.createTempDirDeletedOnExit()
    localPath = tmpDir.toPath().resolve("local-3.3.0.xml")
    `when`(mockUrlProvider.getLocalConfig(ArgumentMatchers.anyString())).thenReturn(localPath)
  }

  override fun tearDown() {
    super.tearDown()
    StudioFlags.WHATS_NEW_ASSISTANT_ENABLED.clearOverride()
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.clearOverride()
  }

  @Test
  fun testDisabled() {
    StudioFlags.WHATS_NEW_ASSISTANT_ENABLED.override(false)

    TestCase.assertFalse(WhatsNewAssistantBundleCreator.shouldShowReleaseNotes())
  }

  @Test
  fun testEnabled() {
    val mockBundler = mock(AssistantBundleCreator::class.java)
    `when`(mockBundler.bundleId).thenReturn(WhatsNewAssistantBundleCreator.BUNDLE_ID)
    `when`(mockBundler.config).thenReturn(URL("file:test.file"))
    WhatsNewAssistantBundleCreator.setTestCreator(mockBundler)

    TestCase.assertTrue(WhatsNewAssistantBundleCreator.shouldShowReleaseNotes())

    WhatsNewAssistantBundleCreator.setTestCreator(null)
  }

  /**
   * Test with a file that exists, simulating good internet connection
   */
  @Test
  fun testDownloadSuccess() {
    // Expected bundle file is server-3.3.0.xml
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(150, bundle.version)
      TestCase.assertEquals("Test What's New from Server", bundle.name)
    }
  }

  /**
   * Test with a file that does not exist, simulating no internet, and also
   * without an already downloaded/unpacked file, so the bundle file will
   * be from the class resource
   */
  @Test
  fun testDownloadDoesNotExist() {
    `when`(mockUrlProvider.getWebConfig(ArgumentMatchers.anyString())).thenReturn(URL("file:server-doesnotexist-3.3.0.xml"))

    // Expected bundle file is defaultresource-3.3.0.xml
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(100, bundle.version)
      TestCase.assertEquals("Test What's New from Class Resource", bundle.name)
    }
  }

  /**
   * First test a downloaded file, then with one that doesn't exist, simulating
   * losing internet connection after having it earlier
   */
  @Test
  fun testDownloadDoesNotExistWithExistingDownloaded() {
    // First expected bundle file is server-3.3.0.xml
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(150, bundle.version)
      TestCase.assertEquals("Test What's New from Server", bundle.name)
    }

    // Change server file to one that doesn't exist, meaning no connection
    `when`(mockUrlProvider.getWebConfig(ArgumentMatchers.anyString())).thenReturn(URL("file:server-doesnotexist-3.3.0.xml"))
    // Expected bundle file is still server-3.3.0.xml because it was downloaded on the first fetch
    val newBundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(newBundle)
    if (newBundle != null) {
      TestCase.assertEquals(150, newBundle.version)
      TestCase.assertEquals("Test What's New from Server", newBundle.name)
    }
  }

  /**
   * Test that disabling the download flag will not fetch from "server"
   */
  @Test
  fun testDownloadFlagDisabled() {
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(false)

    // Expected bundle file is defaultresource-3.3.0.xml
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(100, bundle.version)
      TestCase.assertEquals("Test What's New from Class Resource", bundle.name)
    }
  }

  @Test
  fun testDownloadTimeout() {
    val mockConnectionOpener = mock(WhatsNewAssistantConnectionOpener::class.java)
    `when`(mockConnectionOpener.openConnection(ArgumentMatchers.isNotNull<URL>(), ArgumentMatchers.anyInt())).thenThrow(TimeoutException())

    // Expected bundle file is defaultresource-3.3.0.xml
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider, mockConnectionOpener)
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(100, bundle.version)
      TestCase.assertEquals("Test What's New from Class Resource", bundle.name)
    }
  }

  /**
   * Test that parseBundle correctly deletes local cache and retries once when the parse fails
   */
  @Test
  fun testParseBundleRetry() {
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(false)
    // Trying to read empty xml will cause parser to throw exception...
    val emptyFile = File(myFixture.testDataPath).resolve("whatsnewassistant/empty.xml")
    FileUtil.copy(emptyFile, localPath.toFile())
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)

    // So parseBundle should delete the empty file and retry, resulting in defaultresource-3.3.0.xml
    val bundle = bundleCreator.getBundle(ProjectManager.getInstance().defaultProject)
    TestCase.assertNotNull(bundle)
    if (bundle != null) {
      TestCase.assertEquals(100, bundle.version)
      TestCase.assertEquals("Test What's New from Class Resource", bundle.name)
    }
  }

  /**
   * Test that WNA bundle creator correctly identifies when an updated config has
   * a higher version field than the previous existing config
   */
  @Test
  fun testNewConfigVersion() {
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(false)

    // Since download is disabled, the current file will be defaultresource-3.3.0.xml, version 100
    val bundleCreator = WhatsNewAssistantBundleCreator(mockUrlProvider)

    // Disabled download means last seen version is from default resource, so 100 = 100
    TestCase.assertFalse(bundleCreator.isNewConfigVersion)

    // After enabling download, the new file will be server-3.3.0.xml, version 150
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(true)
    TestCase.assertTrue(bundleCreator.isNewConfigVersion)

    // And running once again should be false because last seen is now 150
    TestCase.assertFalse(bundleCreator.isNewConfigVersion)

    // Disabling download again should use local-3.3.0.xml, cached from the download, version 150
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(false)
    TestCase.assertFalse(bundleCreator.isNewConfigVersion)
  }
}
