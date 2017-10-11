/*
 * =================================================
 * Copyright 2009 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytocomment

import com.tagtraum.audiokern.AudioSong
import com.tagtraum.audiokern.key.Key
import com.tagtraum.beatunes.KeyTextRenderer
import com.tagtraum.beatunes.analysis.AnalysisException
import com.tagtraum.beatunes.analysis.SongAnalysisTask
import com.tagtraum.beatunes.analysis.Task
import com.tagtraum.beatunes.keyrenderer.DefaultKeyTextRenderer
import org.jruby.RubyObject
import org.python.core.PyProxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.Entity

/**
 * Copies tonal key info to the comments field using the configured renderer.
 * Note that this functionality is already built into beaTunes
 * (starting with [version 4.5](http://blog.beatunes.com/2015/08/looking-good-beatunes-45.html)).
 * This plugin therefore only serves demo purposes.
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 */

// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
class KeyToComment : SongAnalysisTask() {
    init {
        // this task does not take long - therefore we ignore it in per task progress bars
        isProgressRelevant = false
    }

    var rendererClass: String
        get() {
            val renderer = getProperty("renderer")
            return renderer ?: DefaultKeyTextRenderer::class.java.name
        }
        set(klass) {
            setProperty("renderer", klass)
        }

    // default to DefaultKeyTextRenderer
    val renderer: KeyTextRenderer
        get() {
            val desiredRenderer = rendererClass
            val renderers = application.pluginManager.getImplementations(KeyTextRenderer::class.java)
            for (renderer in renderers) {
                val rendererClass = getClassName(renderer)
                if (rendererClass == desiredRenderer) return renderer
            }
            return application.pluginManager.getImplementation(DefaultKeyTextRenderer::class.java)
        }

    /**
     * Returns a verbose description of the task in HTML format. This is shown in the
     * Analysis Options dialog (left pane).
     *
     * @return verbose HTML description.
     */
    override fun getDescription(): String {
        return "<h1>Key To Comment</h1><p>Copies the tonal key (if it exists) to the comment field using the configured format.</p>"
    }

    /**
     * This will be the displayed name of the analysis task.
     *
     * @return HTML string
     */
    override fun getName(): String {
        return "<html>Copy key to<br>comment field</html>"
    }

    /**
     * This is where the actual work occurs. This method is called by beaTunes when
     * this task is processed in the analysis/task queue.
     *
     * @throws AnalysisException if something goes wrong.
     */
    @Throws(AnalysisException::class)
    override fun runBefore(task: Task?) {
        // check whether we can skip this step altogether
        if (skip()) {
            if (LOG.isDebugEnabled) LOG.debug("Skipped " + song)
            return
        }
        // get the song object
        val song = song
        // get the new comment
        val newComments = getNewComments(song)
        if (LOG.isDebugEnabled) LOG.debug("Setting new comments to: " + newComments)
        // store new comment - the new value is automatically persisted and the UI is updated.
        song.comments = newComments
    }

    /**
     * Indicates, whether this task can be skipped.
     *
     * @return true or false
     */
    override fun skip(): Boolean {
        val song = song
        val comments = song.comments
        val commentsKey = getKey(comments)
        val renderedKey = renderer.toKeyString(song.key)
        val skip = commentsKey != null && commentsKey == renderedKey
        if (LOG.isDebugEnabled) LOG.debug("Skipping $song ...")
        return skip
    }

    /**
     * Creates a new comment string.
     *
     * @param song song
     * @return new comment (with key, if the song has a key)
     */
    private fun getNewComments(song: AudioSong): String {
        var comments = if (song.comments == null) "" else song.comments
        if (hasCommentsKey(comments)) {
            comments = removeCommentsKey(comments)
        }
        if (song.key != null) {
            comments = addCommentsKey(comments, song.key)
        }
        return comments
    }

    /**
     * Indicates whether this comment contains a key.
     *
     * @param comments comment
     * @return true, if the comment contains a key
     */
    private fun hasCommentsKey(comments: String): Boolean {
        return getKey(comments) != null
    }

    /**
     * Extracts a key out of a comment string.
     *
     * @param comments comment
     * @return key or `null`, if not found
     */
    private fun getKey(comments: String?): String? {
        val keyString: String?
        if (comments == null || comments.length < KEY_START_MARKER.length + KEY_END_MARKER.length) {
            keyString = null
        } else {
            val start = comments.indexOf(KEY_START_MARKER)
            if (start == -1) {
                keyString = null
            } else {
                val end = comments.indexOf(KEY_END_MARKER, start)
                if (end == -1) {
                    keyString = null
                } else {
                    keyString = comments.substring(start + KEY_START_MARKER.length, end)
                    //keyString = KeyFactory.parseTKEY(key);
                }
            }
        }
        return keyString
    }

    /**
     * Removes a key from a comment string.
     *
     * @param comments comment
     * @return comment without the key
     */
    private fun removeCommentsKey(comments: String): String {
        val start = comments.indexOf(KEY_START_MARKER)
        val end = comments.indexOf(KEY_END_MARKER, start)
        return if (comments.length > end) comments.substring(0, start) + comments.substring(end + 1) else comments.substring(0, start)
    }

    /**
     * Adds a key to a comment.
     *
     * @param comments comment
     * @param key key
     * @return new comment with key
     */
    private fun addCommentsKey(comments: String, key: Key): String {
        return comments + KEY_START_MARKER + renderer.toKeyString(key) + KEY_END_MARKER
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(KeyToComment::class.java)
        private val KEY_START_MARKER = "KEY:"
        private val KEY_END_MARKER = ";"

        /**
         * Ruby and Python object's classnames are not the same after the JVM exists.
         * Therefore we have to get their type's name, which is persistent.
         *
         * @param renderer renderer
         * @return classname
         */
        fun getClassName(renderer: KeyTextRenderer): String {
            val classname: String
            if (renderer is RubyObject) {
                classname = "__jruby." + (renderer as RubyObject).metaClass.name
            } else if (renderer is PyProxy) {
                classname = "__jython." + (renderer as PyProxy)._getPyInstance().type.name
            } else {
                classname = renderer.javaClass.name
            }
            return classname
        }
    }

}

