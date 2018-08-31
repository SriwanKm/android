/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.npw.assetstudio.ui;

import com.android.tools.adtui.ImageComponent;
import com.android.tools.idea.npw.assetstudio.GeneratedIcon;
import com.android.tools.idea.npw.assetstudio.GeneratedImageIcon;
import com.android.tools.idea.npw.assetstudio.IconCategory;
import com.android.tools.idea.npw.assetstudio.IconGenerator;
import com.android.tools.idea.npw.assetstudio.icon.CategoryIconMap;
import com.android.tools.idea.npw.assetstudio.icon.IconGeneratorResult;
import com.android.utils.Pair;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A panel which shows the icons generated by an {@link IconGenerator}.
 */
@SuppressWarnings("UseJBColor")
public class PreviewIconsPanel extends JPanel {
  private static final String GENERATED_IMAGE_KEY = "GeneratedImage";

  @NotNull private final Theme myTheme;
  @NotNull private final Map<String, ImageComponent> myIconImages = new HashMap<>();

  @Nullable private CategoryIconMap.Filter myFilter;

  private JPanel myRootPanel;
  private JPanel myIconsPanel;
  private JBLabel myTitleLabel;

  public PreviewIconsPanel(@NotNull String title, @NotNull Theme theme) {
    super(new BorderLayout());
    add(myRootPanel);

    myTitleLabel.setText(title);
    myTitleLabel.setVisible(!title.isEmpty());

    myTheme = theme;
    myRootPanel.setBackground(myTheme.getMainColor());
    myRootPanel.setOpaque(myTheme != Theme.TRANSPARENT);
    myTitleLabel.setForeground(myTheme.getAltColor());
    setName("PreviewIconsPanel"); // for UI tests
  }

  public PreviewIconsPanel(@NotNull String title, @NotNull Theme theme, @NotNull CategoryIconMap.Filter filter) {
    this(title, theme);
    myFilter = filter;
  }

  private static void showPreviewImageImpl(@NotNull ImageComponent imageComponent, @NotNull BufferedImage sourceImage) {
    // Store the original image as a property to ensure we can get it later when
    // displayed in the other preview panel.
    imageComponent.putClientProperty(GENERATED_IMAGE_KEY, sourceImage);
    JBImageIcon icon = IconUtil.createImageIcon(sourceImage);
    Dimension d = new Dimension(icon.getIconWidth(), icon.getIconHeight());
    imageComponent.setPreferredSize(d);
    imageComponent.setMinimumSize(d);
    imageComponent.setIcon(icon);
  }

  /**
   * Returns the list of generated preview images.
   */
  // TODO  Renaud Paquay wrote that this method could be removed, but it's not clear to me what to replace it with.
  @NotNull
  public List<IconPreviewInfo> getIconPreviewInfos() {
    List<IconPreviewInfo> result = new ArrayList<>();

    JPanel icons = myIconsPanel;
    for (Component icon : icons.getComponents()) {
      JPanel iconPanel = (JPanel)icon;
      JBLabel label = (JBLabel)iconPanel.getComponent(0);
      ImageComponent imageComponent = (ImageComponent)iconPanel.getComponent(1);

      IconPreviewInfo previewInfo = new IconPreviewInfo();
      previewInfo.setLabel(label.getText());
      BufferedImage bufferedImage = (BufferedImage)imageComponent.getClientProperty(GENERATED_IMAGE_KEY);
      if (bufferedImage != null) {
        previewInfo.setImage(bufferedImage);
      }
      previewInfo.setImageBorder(imageComponent.getBorder());
      previewInfo.setImageBackground(iconPanel.getBackground());
      previewInfo.setImageOpaque(iconPanel.isOpaque());

      result.add(previewInfo);
    }
    return result;
  }

  public static class IconPreviewInfo {
    @Nullable private String myLabel;
    @Nullable private BufferedImage myImage;
    @Nullable private Border myImageBorder;
    private boolean myImageOpaque;
    @Nullable private Color myImageBackground;

    public void setLabel(@Nullable String label) {
      myLabel = label;
    }

    @Nullable
    public String getLabel() {
      return myLabel;
    }

    public void setImageBackground(@Nullable Color imageBackground) {
      myImageBackground = imageBackground;
    }

    @Nullable
    public Color getImageBackground() {
      return myImageBackground;
    }

    public void setImageOpaque(boolean imageOpaque) {
      myImageOpaque = imageOpaque;
    }

    public boolean isImageOpaque() {
      return myImageOpaque;
    }

    public void setImage(@Nullable BufferedImage image) {
      myImage = image;
    }

    @Nullable
    public BufferedImage getImage() {
      return myImage;
    }

    public void setImageBorder(@Nullable Border imageBorder) {
      myImageBorder = imageBorder;
    }

    @Nullable
    public Border getImageBorder() {
      return myImageBorder;
    }
  }

  /** Updates the preview images in this panel using values generated by an {@link IconGenerator}.
   *
   * <p>Note: This panel lazily instantiates its components based on the values in the map the first
   * time this is called. Additional calls should pass in maps with the same keys. This happens
   * automatically as long as you keep generating icons with consistent options.
   */
  public void showPreviewImages(@NotNull IconGeneratorResult iconGeneratorResult) {
    // Default implementation displays all preview icons of type "GeneratedImageIcon".
    Collection<GeneratedIcon> generatedIcons = iconGeneratorResult.getIcons();
    List<Pair<String, BufferedImage>> list = generatedIcons.stream()
        .filter(icon -> icon instanceof GeneratedImageIcon && icon.getCategory() == IconCategory.PREVIEW)
        .map(icon -> (GeneratedImageIcon)icon)
        .sorted(Comparator.comparingInt(icon -> -icon.getDensity().getDpiValue()))
        .map(icon -> Pair.of(icon.getName(), icon.getImage()))
        .collect(Collectors.toList());
    showPreviewImagesImpl(list);
  }

  protected void showPreviewImagesImpl(@NotNull List<Pair<String, BufferedImage>> images) {
    initializeIconComponents(images.stream().map(Pair::getFirst).collect(Collectors.toList()));
    images.forEach(pair -> showPreviewImageImpl(myIconImages.get(pair.getFirst()), pair.getSecond()));
  }

  private void initializeIconComponents(@NotNull List<String> labels) {
    // Ensure the list of components matches the list of labels, updating
    // incrementally.
    int labelIndex = 0;
    for (; labelIndex < labels.size(); labelIndex++) {
      String label = labels.get(labelIndex);
      int iconIndex = findIconPanel(labelIndex, label);
      if (iconIndex < 0) {
        // The is no existing panel for this label, create a new one
        JPanel iconPanel = new JPanel(new VerticalFlowLayout(false, false));
        iconPanel.setName("IconPanel"); // for UI Tests
        iconPanel.setBackground(myTheme.getMainColor());
        iconPanel.setOpaque(myTheme != Theme.TRANSPARENT);

        JBLabel title = new JBLabel(label);
        title.setForeground(myTheme.getAltColor());
        iconPanel.add(title);

        ImageComponent iconImage = new ImageComponent(null);
        iconImage.setBorder(new LineBorder(myTheme.getAltColor()));
        iconImage.setOpaque(false);
        iconPanel.add(iconImage);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_START;

        // Store the new panel exactly at "labelIndex"
        myIconsPanel.add(iconPanel, c, labelIndex);
        myIconImages.put(label, iconImage);
      }
      else {
        // Remove icons from [labelIndex, iconIndex[
        for (int i = labelIndex; i < iconIndex; i++) {
          removeIconPanel(i);
        }
      }
    }
    assert labelIndex == labels.size();
    assert myIconsPanel.getComponentCount() >= labels.size();
    assert myIconImages.size() == myIconsPanel.getComponentCount();

    // Remove remaining of obsolete panels (i.e. no equivalent in new labels)
    while (myIconsPanel.getComponentCount() > labels.size()) {
      removeIconPanel(labels.size());
    }
  }

  private void removeIconPanel(int i) {
    JPanel iconPanel = (JPanel)myIconsPanel.getComponent(i);
    String iconLabel = ((JLabel)iconPanel.getComponent(0)).getText();
    myIconsPanel.remove(i);
    myIconImages.remove(iconLabel);
  }

  private int findIconPanel(int startIndex, String label) {
    for (int i = startIndex; i < myIconsPanel.getComponentCount(); i++) {
      JPanel iconPanel = (JPanel)myIconsPanel.getComponent(i);
      String iconLabel = ((JLabel)iconPanel.getComponent(0)).getText();
      if (Objects.equals(label, iconLabel)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Color themes which are useful for acting as backdrops to our various icon types.
   */
  public enum Theme {
    DARK(Color.BLACK, Color.WHITE),
    LIGHT(Color.WHITE, Color.BLACK),
    GRAY(Color.DARK_GRAY, Color.LIGHT_GRAY),
    TRANSPARENT(UIUtil.TRANSPARENT_COLOR, JBColor.BLACK);

    private final Color myMainColor;
    private final Color myAltColor;

    Theme(@NotNull Color mainColor, @NotNull Color altColor) {
      myMainColor = mainColor;
      myAltColor = altColor;
    }

    /**
     * The theme's main color, used for the background.
     */
    @NotNull
    public Color getMainColor() {
      return myMainColor;
    }

    /**
     * The theme's alt color, used for rendering lines and text on the background.
     */
    @NotNull
    public Color getAltColor() {
      return myAltColor;
    }
  }
}
