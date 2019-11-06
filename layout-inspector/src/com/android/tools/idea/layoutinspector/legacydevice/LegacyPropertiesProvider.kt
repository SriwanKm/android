/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.layoutinspector.legacydevice

import com.android.SdkConstants
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.resources.ResourceType
import com.android.resources.ResourceUrl
import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.properties.InspectorPropertyItem
import com.android.tools.idea.layoutinspector.properties.PropertiesProvider
import com.android.tools.property.panel.api.PropertiesTable
import com.google.common.collect.HashBasedTable

private const val ATTR_TOP = "top"
private const val ATTR_BOTTOM = "bottom"
private const val ATTR_LEFT = "left"
private const val ATTR_RIGHT = "right"
private const val ATTR_SCROLL_X = "scrollX"
private const val ATTR_SCROLL_Y = "scrollY"
private const val ATTR_X = "x"
private const val ATTR_Y = "y"
private const val ATTR_Z = "z"

/**
 * A [PropertiesProvider] that can handle pre-api 29 devices.
 *
 * Loads the properties
 */
class LegacyPropertiesProvider : PropertiesProvider {
  private var properties = mapOf<Long, PropertiesTable<InspectorPropertyItem>>()

  override val resultListeners = mutableListOf<(PropertiesProvider, ViewNode, PropertiesTable<InspectorPropertyItem>) -> Unit>()

  override fun requestProperties(view: ViewNode) {
    val viewProperties = properties[view.drawId] ?: PropertiesTable.emptyTable()
    resultListeners.forEach { it(this, view, viewProperties) }
  }

  class Updater {
    private var temp = mutableMapOf<Long, PropertiesTable<InspectorPropertyItem>>()

    fun apply(provider: LegacyPropertiesProvider) {
      provider.properties = temp
    }

    fun parseProperties(view: ViewNode, data: String) {
      val parent = view.parent
      var start = 0
      var stop: Boolean
      val table = HashBasedTable.create<String, String, InspectorPropertyItem>()
      do {
        val index = data.indexOf('=', start)
        val fullName = data.substring(start, index)
        val colonIndex = fullName.indexOf(':')
        val group = fullName.substring(0, Integer.max(0, colonIndex))
        val rawName = fullName.substring(colonIndex + 1)
        val name = PropertyMapper.mapPropertyName(rawName)
        val index2 = data.indexOf(',', index + 1)
        val length = Integer.parseInt(data.substring(index + 1, index2))
        start = index2 + 1 + length

        val rawValue = data.substring(index2 + 1, index2 + 1 + length)
        val type = PropertyMapper.mapPropertyType(rawName, name, rawValue)
        val value = PropertyMapper.mapPropertyValue(name, type, rawValue)
        if (group != "theme" && !PropertyMapper.exclude(rawName)) {
          val property = InspectorPropertyItem(SdkConstants.ANDROID_URI, name, name, type, value, false, null, view, null)
          table.put(property.namespace, property.name, property)
        }
        stop = start >= data.length
        if (!stop) {
          start += 1
        }
      }
      while (!stop)

      view.x = (table.remove(SdkConstants.ANDROID_URI, ATTR_LEFT)?.value?.toInt() ?: 0) + (parent?.let { it.x - it.scrollX } ?: 0)
      view.y = (table.remove(SdkConstants.ANDROID_URI, ATTR_TOP)?.value?.toInt() ?: 0) + (parent?.let { it.y - it.scrollY } ?: 0)
      view.scrollX = table[SdkConstants.ANDROID_URI, ATTR_SCROLL_X]?.value?.toInt() ?: 0
      view.scrollY = table[SdkConstants.ANDROID_URI, ATTR_SCROLL_Y]?.value?.toInt() ?: 0
      view.width = table.remove(SdkConstants.ANDROID_URI, SdkConstants.ATTR_WIDTH)?.value?.toInt() ?: 0
      view.height = table.remove(SdkConstants.ANDROID_URI, SdkConstants.ATTR_HEIGHT)?.value?.toInt() ?: 0
      view.textValue = table[SdkConstants.ANDROID_URI, SdkConstants.ATTR_TEXT]?.value ?: ""
      val url = table[SdkConstants.ANDROID_URI, SdkConstants.ATTR_ID]?.value?.let { ResourceUrl.parse(it) }
      view.viewId = url?.let { ResourceReference(ResourceNamespace.TODO(), ResourceType.ID, it.name) }

      // Remove other attributes that we already have elsewhere:
      table.remove(SdkConstants.ANDROID_URI, ATTR_X)
      table.remove(SdkConstants.ANDROID_URI, ATTR_Y)
      table.remove(SdkConstants.ANDROID_URI, ATTR_Z)
      table.remove(SdkConstants.ANDROID_URI, ATTR_BOTTOM)
      table.remove(SdkConstants.ANDROID_URI, ATTR_RIGHT)

      temp[view.drawId] = PropertiesTable.create(table)
    }
  }
}
