package org.key_project.ide

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import org.antlr.v4.runtime.CharStreams
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.reactfx.EventStreams
import tornadofx.getValue
import tornadofx.setValue
import java.nio.file.Path
import java.time.Duration
import java.util.*
import kotlin.io.path.extension

object Editors {
    @JvmStatic
    fun getLanguageForFilename(file: Path) = getEditorForSuffix(file.extension)

    @JvmStatic
    fun getEditorForSuffix(suffix: String): Language? =
        when (suffix) {
            "key" -> KeyLanguage
            //"java", "jml" -> JavaLanguage
            else -> null
        }
}


class Editor(val ctx: Context)  {
    val editor = CodeArea("")
    val scrollPane =VirtualizedScrollPane(editor)
    val ui = scrollPane

    val dirtyProperty = SimpleBooleanProperty(this, "dirty", false)
    var dirty by dirtyProperty

    val filenameProperty = SimpleObjectProperty<Path>(this, "filename", null)
    var filename by filenameProperty

    val languageProperty = SimpleObjectProperty<Language>(this, "language", null)
    var language by languageProperty

    val title = Bindings.createStringBinding(
        { (filename?.fileName?.toString() ?: "unknown_file") + (if (dirty) "*" else "") },
        dirtyProperty,
        filenameProperty
    )

    init {
        editor.paragraphGraphicFactory = LineNumberFactory.get(editor)
        //editor. = true

        filenameProperty.addListener { _, _, new ->
            language = Editors.getLanguageForFilename(new)
        }

        editor.textProperty().addListener { _, oldText, newText: String ->
            dirty = dirty || oldText != newText
            onTextLightComputation()
        }

        languageProperty.addListener { _, _, _ ->
            onTextLightComputation()
            onTextChangeHeavyComputation()
        }

        EventStreams.changesOf(editor.textProperty())
            .successionEnds(Duration.ofMillis(500)) //wait 500ms before responds
            .forgetful()
            .subscribe { onTextChangeHeavyComputation() }
    }

    private fun onTextChangeHeavyComputation() {
        val text = editor.text
        language?.let { language ->
            if (text.isNotEmpty()) {
                val seq = arrayListOf<IssueEntry>()
                val p = language.contextFactory(CharStreams.fromString(editor.text), IssueErrorListener(seq))

                language.computeOutline(p, this)?.let { root ->
                    ctx.get<FileOutline>().outlineTree.root = root
                }

                language.computeIssues(p, filename, this).let { root ->
                    seq.addAll(root)
                }

                ctx.get<IssueList>().view.items.setAll(seq)
            }
        }
    }

    private fun onTextLightComputation() {
        language?.also { lang ->
            if (editor.text.isNotBlank())
                editor.setStyleSpans(0, computeHighlighting(lang))
        }
    }


    fun computeHighlighting(language: Language): StyleSpans<Collection<String>>? {
        val text = editor.text
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        val lexer = language.lexerFactory(CharStreams.fromString(text))
        do {
            val tok = lexer.nextToken()
            if (tok.type == -1) break
            val typ = language.getStyleClass(tok.type)// lexer.vocabulary.getSymbolicName(tok.type)
            spansBuilder.add(Collections.singleton(typ), tok.text.length)
        } while (tok.type != -1)
        return spansBuilder.create()
    }
}
