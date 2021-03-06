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
package com.android.tools.idea.uibuilder.handlers.motion.attributeEditor;

import com.android.tools.idea.uibuilder.handlers.motion.timeline.MotionSceneModel.CustomAttributes.Type;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;

public class NewCustomAttributePanel extends DialogWrapper {
  private JTextField myAttributeNameEditor;
  private JTextField myInitialValueEditor;
  private JPanel myContentPanel;
  private JComboBox myDataType;
  private DefaultComboBoxModel<Type> myModel;

  public NewCustomAttributePanel() {
    super(false);
    myModel = new DefaultComboBoxModel<>();
    Arrays.stream(Type.values()).forEach(type -> myModel.addElement(type));
    myModel.setSelectedItem(Type.CUSTOM_STRING);
    myDataType.setModel(myModel);
    myDataType.setEditable(false);
    init();
  }

  @NotNull
  @Override
  protected JComponent createCenterPanel() {
    return myContentPanel;
  }

  @NotNull
  public String getAttributeName() {
    return myAttributeNameEditor.getText();
  }

  @NotNull
  public String getInitialValue() {
    return myInitialValueEditor.getText();
  }

  @NotNull
  public Type getType() {
    return (Type)myDataType.getSelectedItem();
  }
}
