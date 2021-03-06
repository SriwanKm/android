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
package com.android.tools.idea.whatsnew.assistant;


import com.android.repository.Revision;
import com.android.testutils.TestUtils;
import com.android.tools.idea.assistant.AssistantBundleCreator;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.util.FutureUtils;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.GeneralSettings;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.http.concurrent.FutureCallback;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Tests for {@link WhatsNewStartupActivity}
 */
public class WhatsNewStartupActivityTest extends AndroidTestCase {
  private static final long TIMEOUT_MILLISECONDS = 30000;

  private WhatsNewAssistantURLProvider mockUrlProvider;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    StudioFlags.WHATS_NEW_ASSISTANT_ENABLED.override(true);
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(true);

    mockUrlProvider = Mockito.mock(WhatsNewAssistantURLProvider.class);
    File serverFile = new File(myFixture.getTestDataPath(), "whatsnewassistant/server-3.3.0.xml");
    Mockito.when(mockUrlProvider.getWebConfig(ArgumentMatchers.anyString())).thenReturn(new URL("file:" + serverFile.getPath()));

    File resourceFile = new File(myFixture.getTestDataPath(), "whatsnewassistant/defaultresource-3.3.0.xml");
    Mockito.when(mockUrlProvider.getResourceFileAsStream(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
      .thenReturn(new URL("file:" + resourceFile.getPath()).openStream());

    File tmpDir = TestUtils.createTempDirDeletedOnExit();
    Path localPath = tmpDir.toPath().resolve("local-3.3.0.xml");
    Mockito.when(mockUrlProvider.getLocalConfig(ArgumentMatchers.anyString())).thenReturn(localPath);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      StudioFlags.WHATS_NEW_ASSISTANT_ENABLED.clearOverride();
      StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.clearOverride();
    }
    finally {
      super.tearDown();
    }
  }

  /**
   * Test that isNewStudioVersion makes correct comparisons between version numbers
   */
  public void testIsNewStudioVersion() {
    WhatsNewStartupActivity wn = new WhatsNewStartupActivity();
    WhatsNewStartupActivity.WhatsNewData data = new WhatsNewStartupActivity.WhatsNewData();

    // no previous version seen, current version > latest available
    assertTrue(wn.isNewStudioVersion(data, new Revision(1, 0, 1, 0)));
    assertEquals(1, Revision.parseRevision("1.1.0.0").compareTo(Revision.parseRevision(data.myRevision)));

    // previous version seen < current version == latest available
    data.myRevision = "1.0.0.0";
    assertTrue(wn.isNewStudioVersion(data, new Revision(1, 1, 0, 0)));
    assertEquals(0, Revision.parseRevision("1.1.0.0").compareTo(Revision.parseRevision(data.myRevision)));

    // previous version seen == current version == latest available
    data.myRevision = "1.1.0.0";
    assertFalse(wn.isNewStudioVersion(data, new Revision(1, 1, 0, 0)));
    assertEquals(0, Revision.parseRevision("1.1.0.0").compareTo(Revision.parseRevision(data.myRevision)));

    // previous version seen < current version < latest available
    data.myRevision = "1.0.0.0";
    assertTrue(wn.isNewStudioVersion(data, new Revision(1, 1, 1, 1)));
    assertEquals(0, Revision.parseRevision("1.1.1.1").compareTo(Revision.parseRevision(data.myRevision)));
  }

  /**
   * Test that asynchronous version checking for What's New Assistant works
   */
  public void testCheckVersionTask() {
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(true);

    WhatsNewAssistantBundleCreator bundleCreator = AssistantBundleCreator.EP_NAME
      .findExtension(WhatsNewAssistantBundleCreator.class);
    bundleCreator.setURLProvider(mockUrlProvider);

    // Future callback, should return true since current version would be -1
    SettableFuture<Boolean> completeFuture = SettableFuture.create();

    // WhatsNewStartupActivity does this normally in production
    new WhatsNewAssistantCheckVersionTask(getProject(), new BooleanCallback(completeFuture)).queue();
    try {
      FutureUtils.pumpEventsAndWaitForFuture(completeFuture, TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
      assertTrue(completeFuture.get());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Failed while waiting for future");
    }
  }

  /**
   * Test that Tip of the Day settings is properly restored when WNA needs to auto-show
   */
  public void testStartupTips() {
    StudioFlags.WHATS_NEW_ASSISTANT_DOWNLOAD_CONTENT.override(false);

    // Tips of the Day should be enabled by default
    assertTrue(GeneralSettings.getInstance().isShowTipsOnStartup());
    SettableFuture<Boolean> completeFutureEnabled = SettableFuture.create();

    // Temporarily disables tips and opens WNA
    WhatsNewStartupActivity.hideTipsAndOpenWhatsNewAssistant(getProject(), new BooleanCallback(completeFutureEnabled));

    // Check that Tips are enabled again
    try {
      FutureUtils.pumpEventsAndWaitForFuture(completeFutureEnabled, TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
      assertTrue(completeFutureEnabled.get());
      assertTrue(GeneralSettings.getInstance().isShowTipsOnStartup());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Failed while waiting for future");
    }

    // Now disable Tips instead
    GeneralSettings.getInstance().setShowTipsOnStartup(false);
    SettableFuture<Boolean> completeFutureDisabled = SettableFuture.create();

    // Temporarily disables tips and opens WNA
    WhatsNewStartupActivity.hideTipsAndOpenWhatsNewAssistant(getProject(), new BooleanCallback(completeFutureDisabled));

    // Check that Tips are still disabled
    try {
      FutureUtils.pumpEventsAndWaitForFuture(completeFutureDisabled, TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
      assertTrue(completeFutureDisabled.get());
      assertFalse(GeneralSettings.getInstance().isShowTipsOnStartup());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Failed while waiting for future");
    }
  }

  private static class BooleanCallback implements FutureCallback<Boolean> {
    private SettableFuture<Boolean> completeFuture;

    BooleanCallback(@NotNull SettableFuture<Boolean> future) {
      completeFuture = future;
    }

    @Override
    public void completed(Boolean result) {
      completeFuture.set(result);
    }

    @Override
    public void failed(Exception ex) {
      completeFuture.set(false);
    }

    @Override
    public void cancelled() {
      completeFuture.set(false);
    }
  }
}
