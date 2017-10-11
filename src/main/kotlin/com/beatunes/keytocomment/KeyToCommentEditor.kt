/*
 * =================================================
 * Copyright 2014 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytocomment

import com.tagtraum.beatunes.BeaTunes
import com.tagtraum.beatunes.KeyTextRenderer
import com.tagtraum.beatunes.analysis.TaskEditor

import javax.swing.*
import java.awt.*
import java.util.prefs.Preferences

/**
 * Configuration editor for [com.beatunes.keytocomment.KeyToComment] task.
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 */
class KeyToCommentEditor : TaskEditor<KeyToComment> {

    private var application: BeaTunes? = null
    private val component = JPanel()
    private val keyTextRendererComboBox = JComboBox<KeyTextRenderer>()
    private val keyFormatLabel = JLabel("Key Format:")

    init {
        this.component.layout = BorderLayout()
        this.component.isOpaque = false
        this.component.add(keyFormatLabel, BorderLayout.WEST)
        this.component.add(keyTextRendererComboBox, BorderLayout.CENTER)
    }

    override fun setApplication(beaTunes: BeaTunes) {
        this.application = beaTunes
    }

    override fun getApplication(): BeaTunes? {
        return application
    }

    override fun init() {
        // this localization key happens to be defined in beaTunes 4.0.4 and later
        this.keyFormatLabel.text = application!!.localize("Key_Format")
        val renderers = application!!.pluginManager.getImplementations(KeyTextRenderer::class.java)
        this.keyTextRendererComboBox.model = DefaultComboBoxModel(
                renderers.toTypedArray()
        )
        this.keyTextRendererComboBox.selectedItem = application!!.generalPreferences.keyTextRenderer
        this.keyTextRendererComboBox.isOpaque = false
        this.keyTextRendererComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val s: String
                if (value is KeyTextRenderer) {
                    s = value.name
                } else {
                    s = ""
                }
                return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus)
            }
        }

        val classname = PREFERENCES.get(ANALYSISOPTIONS_KEY_RENDERER, application!!.generalPreferences.keyTextRenderer.javaClass.name)
        for (renderer in renderers) {
            val rendererClassname = KeyToComment.getClassName(renderer)
            if (rendererClassname == classname) {
                keyTextRendererComboBox.selectedItem = renderer
                break
            }
        }
        this.component.addPropertyChangeListener("enabled") { evt ->
            val enabled = evt.newValue as Boolean
            keyTextRendererComboBox.isEnabled = enabled
            keyFormatLabel.isEnabled = enabled
        }
    }

    override fun getComponent(): JComponent {
        return component
    }

    override fun setTask(keyToComment: KeyToComment) {
        val rendererClass = keyToComment.rendererClass
        for (i in 0..keyTextRendererComboBox.itemCount - 1) {
            val renderer = keyTextRendererComboBox.getItemAt(i)
            if (KeyToComment.getClassName(renderer).equals(rendererClass)) {
                keyTextRendererComboBox.selectedIndex = i
                break
            }
        }
    }

    override fun getTask(keyToComment: KeyToComment): KeyToComment {
        val renderer = keyTextRendererComboBox.getItemAt(keyTextRendererComboBox.selectedIndex)
        val classname = KeyToComment.getClassName(renderer)
        keyToComment.rendererClass = classname
        PREFERENCES.put(ANALYSISOPTIONS_KEY_RENDERER, classname)
        return keyToComment
    }

    override fun getTask(): KeyToComment {
        val keyToComment = KeyToComment()
        return getTask(keyToComment)
    }

    companion object {

        private val PREFERENCES = java.util.prefs.Preferences.userNodeForPackage(KeyToCommentEditor::class.java)
        private val ANALYSISOPTIONS_KEY_RENDERER = "analysisoptions.key.renderer"
    }
}
