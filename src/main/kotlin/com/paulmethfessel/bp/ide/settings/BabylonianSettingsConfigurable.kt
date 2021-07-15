package com.paulmethfessel.bp.ide.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.xmlb.XmlSerializerUtil
import javax.swing.JComponent
import javax.swing.JPanel

class BabylonianSettingsConfigurable: Configurable {
    private var component: BabylonianSettingsComponent? = null

    override fun createComponent(): JComponent? {
        component = BabylonianSettingsComponent()
        return component?.panel
    }

    override fun isModified() =
        babylonianSettings.graalPath != component?.graalPath
                || babylonianSettings.startWithDebugger != component?.startWithDebugger

    override fun apply() {
        component?.let {
            babylonianSettings.graalPath = it.graalPath
            babylonianSettings.startWithDebugger = it.startWithDebugger
        }
    }

    override fun reset() {
        component?.apply {
            graalPath = babylonianSettings.graalPath
            startWithDebugger = babylonianSettings.startWithDebugger
        }
    }

    override fun disposeUIResources() {
        component = null
    }

    override fun getPreferredFocusedComponent() = component?.preferredFocusedComponent

    override fun getDisplayName() = "Babylonian"
}

@State(
    name = "com.paulmethfessel.bp.ide.settings.BabylonianSettingsState",
    storages = [Storage("BabylonianPluginSettings.xml")]
)
class BabylonianSettingsState: PersistentStateComponent<BabylonianSettingsState>, Disposable {
    var graalPath: String = ""
    var startWithDebugger = false

    override fun getState() = this

    override fun loadState(state: BabylonianSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun dispose() {}
}

val babylonianSettings get() = service<BabylonianSettingsState>()

class BabylonianSettingsComponent {
    val panel: JPanel
    private val graalPathText = buildGraalPathField()
    private val startWithDebuggerCheckbox = JBCheckBox("Start GraalVM with debugger?")

    val preferredFocusedComponent = graalPathText

    var graalPath
        get() = graalPathText.text
        set(value) { graalPathText.text = value }
    var startWithDebugger
        get() = startWithDebuggerCheckbox.isSelected
        set(value) { startWithDebuggerCheckbox.isSelected = value }

    init {
        panel = FormBuilder.createFormBuilder().apply {
            addLabeledComponent(JBLabel("Path to GraalVM bin folder"), graalPathText, 1, true)
            addComponent(startWithDebuggerCheckbox, 1)
            addComponentFillVertically(JPanel(), 0)
        }.panel
    }

    private fun buildGraalPathField(): TextFieldWithBrowseButton {
        val field = TextFieldWithBrowseButton()
        val chooser = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        field.addBrowseFolderListener("GraalVM Path", "Choose directory of GraalVM/bin", null, chooser)
        return field
    }
}
