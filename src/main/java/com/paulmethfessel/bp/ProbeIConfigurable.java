package com.paulmethfessel.bp;

import com.intellij.codeInsight.hints.ChangeListener;
import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.paulmethfessel.bp.ide.decorators.JavaPanelHelper;

import javax.swing.*;

/**
 * ImmediateConfigurable cannot be extended in Kotlin for jvm 1.8
 */
public class ProbeIConfigurable implements ImmediateConfigurable {
	public JComponent createComponent(ChangeListener listener) {
		return new JavaPanelHelper().createPanel();
	}
}
