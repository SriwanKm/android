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
package com.android.tools.idea.assistant

import com.android.tools.idea.assistant.datamodel.TutorialBundleData
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.apache.http.concurrent.FutureCallback
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.xml.bind.JAXBException

class AssistantGetBundleFromConfigTask(
  project: Project,
  private val config: URL,
  private val uiCallback: FutureCallback<TutorialBundleData>
): Task.Backgroundable(project, "Loading assistant content...") {

  private lateinit var bundleData: TutorialBundleData

  override fun run(indicator: ProgressIndicator) {
    bundleData = getBundle(config)
  }

  override fun onSuccess() {
    super.onSuccess()
    uiCallback.completed(bundleData)
  }

  override fun onCancel() {
    super.onCancel()
    uiCallback.cancelled()
  }

  override fun onThrowable(error: Throwable) {
    uiCallback.failed(RuntimeException(error))
  }

  private fun getBundle(config: URL): TutorialBundleData {
    val inputStream = config.openStream()
    val bundle: TutorialBundleData
    bundle = DefaultTutorialBundle.parse(inputStream!!)
    inputStream.close()
    return bundle
  }
}