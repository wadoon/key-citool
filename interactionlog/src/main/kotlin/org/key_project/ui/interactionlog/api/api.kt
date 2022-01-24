package org.key_project.ui.interactionlog.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.ui.AbstractMediatorUserInterfaceControl
import org.key_project.ui.interactionlog.InteractionRecorder
import org.key_project.ui.interactionlog.model.InteractionLog
import java.awt.Color
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*
import javax.swing.Icon

/**
 * @author weigl
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
abstract class Interaction : Serializable, Markdownable, Scriptable, Reapplicable {
    @JsonIgnore
    var graphicalStyle = InteractionGraphicStyle()
        protected set

    var created = LocalDateTime.now()

    var isFavoured = false

    class InteractionGraphicStyle {
        var icon: Icon? = null
        var backgroundColor: Color? = null
        var foregroundColor: Color? = null
    }
}

/**
 * A interaction recoder listener receives interactions to store them.
 */
interface InteractionRecorderListener {
    fun onInteraction(recorder: InteractionRecorder, log: InteractionLog, event: Interaction) {}
    fun onNewInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {}
    fun onDisposeInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {}
}

/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Scriptable {
    @get:JsonIgnore
    val proofScriptRepresentation: String
        get() = "// Unsupported interaction: $javaClass\n"
}

/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Reapplicable {
    @Throws(Exception::class)
    fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        throw UnsupportedOperationException()
    }

    @Throws(Exception::class)
    fun reapplyRelaxed(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        throw UnsupportedOperationException()
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (08.05.19)
 */
interface Markdownable {
    @get:JsonIgnore
    val markdown: String
        get() = String.format("**Unsupported interaction: %s**%n%n", this.javaClass.name)
}
