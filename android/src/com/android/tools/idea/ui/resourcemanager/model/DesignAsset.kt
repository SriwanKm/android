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
package com.android.tools.idea.ui.resourcemanager.model

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceValue
import com.android.ide.common.resources.ResourceItem
import com.android.ide.common.resources.ResourceMergerItem
import com.android.ide.common.resources.ResourceResolver
import com.android.ide.common.resources.configuration.DensityQualifier
import com.android.ide.common.resources.configuration.ResourceQualifier
import com.android.resources.ResourceType
import com.android.tools.idea.res.getSourceAsVirtualFile
import com.android.tools.idea.ui.resourcemanager.importer.QualifierMatcher
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

val externalResourceNamespace = ResourceNamespace.fromPackageName("external.design.resource")
private val LOG = Logger.getInstance(DesignAsset::class.java)

/**
 * A Design asset on disk.
 *
 * This class helps to interface a project's resource with a external file
 */
data class DesignAsset(
  val file: VirtualFile,
  var qualifiers: List<ResourceQualifier>,
  val type: ResourceType,
  val name: String = file.nameWithoutExtension,
  val resourceItem: ResourceItem = ResourceMergerItem(name, externalResourceNamespace, type, null, "external")

) {

  companion object {
    fun fromResourceItem(resourceItem: ResourceItem): DesignAsset? {
      val file = resourceItem.getSourceAsVirtualFile() ?: return null
      return DesignAsset(
        file = file,
        qualifiers = resourceItem.configuration.qualifiers.toList(),
        type = resourceItem.type,
        name = resourceItem.name,
        resourceItem = resourceItem)
    }
  }

  /**
   * Returns the human readable (KB, MB) file size if the [DesignAsset] is a file (e.g layout, drawables)
   * and not contained in a file (e.g colors).
   */
  fun getDisplayableFileSize(): String {
    if (resourceItem.isFileBased) {
      return StringUtil.formatFileSize(file.length)
    }
    return ""
  }
}

/**
 * Represents a set of design assets on disk grouped by base name.
 *
 * For example, fr/icon@2x.png, fr/icon.jpg  and en/icon.png will be
 * gathered in the same DesignAssetSet under the name "icon"
 */
data class DesignAssetSet(
  val name: String,
  var designAssets: List<DesignAsset>
) {

  /**
   * Return the asset in this set with the highest density
   */
  fun getHighestDensityAsset(): DesignAsset {
    return designAssets.maxBy { asset ->
      asset.qualifiers
        .filterIsInstance<DensityQualifier>()
        .map { densityQualifier -> densityQualifier.value.dpiValue }
        .singleOrNull() ?: 0
    } ?: designAssets[0]
  }
}

/**
 * Find all the [DesignAssetSet] in the given directory
 *
 * @param supportedTypes The file types supported for importation
 */
fun getAssetSets(
  directory: VirtualFile,
  supportedTypes: Set<String>,
  qualifierMatcher: QualifierMatcher
): List<DesignAssetSet> {
  return getDesignAssets(directory, supportedTypes, directory, qualifierMatcher)
    .groupBy { designAsset -> designAsset.name }
    .map { (drawableName, designAssets) -> DesignAssetSet(drawableName, designAssets) }
    .toList()
}

fun getDesignAssets(
  directory: VirtualFile,
  supportedTypes: Set<String>,
  root: VirtualFile,
  qualifierMatcher: QualifierMatcher
): List<DesignAsset> {
  return directory.children
    .filter { it.isDirectory || supportedTypes.contains(it.extension) }
    .flatMap {
      if (it.isDirectory) getDesignAssets(it, supportedTypes, root, qualifierMatcher)
      else listOf(createAsset(it, root, qualifierMatcher))
    }
}

private fun createAsset(child: VirtualFile, root: VirtualFile, matcher: QualifierMatcher): DesignAsset {
  val relativePath = VfsUtil.getRelativePath(child, root) ?: child.path
  val (resourceName, qualifiers1) = matcher.parsePath(relativePath)
  return DesignAsset(child, qualifiers1.toList(), ResourceType.DRAWABLE, resourceName)
}

fun ResourceResolver.resolveValue(designAsset: DesignAsset): ResourceValue? {
  val resolvedValue = resolveResValue(designAsset.resourceItem.resourceValue)
  if (resolvedValue == null) {
    LOG.warn("${designAsset.resourceItem.name} couldn't be resolved")
  }
  return resolvedValue
}