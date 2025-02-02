/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.laf.macos;

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI;
import com.intellij.ui.Gray;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public final class MacIntelliJProgressBarUI extends DarculaProgressBarUI {
  public static final Gray GRAPHITE_START_COLOR = Gray.xD4;
  @SuppressWarnings("UseJBColor") public static final Color GRAPHITE_END_COLOR = new Color(0x989a9e);

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new MacIntelliJProgressBarUI();
  }

  @Override
  protected Color getFinishedColor() {
    return UIUtil.isGraphite() ? GRAPHITE_END_COLOR : super.getFinishedColor();
  }

  @Override
  protected Color getStartColor() {
    return UIUtil.isGraphite() ? GRAPHITE_START_COLOR : super.getStartColor();
  }

  @Override
  protected Color getEndColor() {
    return UIUtil.isGraphite() ? getFinishedColor() : super.getEndColor();
  }
}
