package org.key_project.ui.interactionlog.algo

import de.uka.ilkd.key.java.Services
import de.uka.ilkd.key.proof.Node
import org.key_project.ui.interactionlog.api.Interaction
import org.key_project.ui.interactionlog.model.InteractionLog
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.function.Function

/**
 * @author weigl
 */
class LogPrinter(private val services: Services) {
    private var w: StringWriter? = null
    private var out: PrintWriter? = null
    var matchExpr = Function<Node, String> { getBranchingLabel(it) }
    private var indent = 0
    private var state: InteractionLog? = null

    /**
     * prints an interaction log as a proof script.
     *
     * @param state a state
     * @return
     */
    fun print(state: InteractionLog): String {
        w = StringWriter()
        out = PrintWriter(w!!)
        this.state = state
        indent = 0
        header()
        body()
        footer()
        return w!!.toString()
    }

    private fun header() {
        out!!.print("script main {")
        ++indent
    }

    private fun body() {
        if (state!!.interactions.size != 0) {
            //HashMap<Interaction, List<Interaction>> tree = state.getInteractionTree();
            //body(tree, state.getInteractions().get(0));
        }
    }

    private fun body(tree: HashMap<Interaction, List<Interaction>>,
                     interaction: Interaction) {

        newline()
        //TODO out.write(interaction.getProofScriptRepresentation(services));

        val children = tree[interaction]
        if (children != null) {
            when (children.size) {
                1 -> body(tree, children[0])
                else -> {
                    newline()
                    out!!.write("cases {")
                    indent++

                    for (c in children) {
                        newline()
                        out!!.write("case \"")
                        //TODO out.write(matchExpr.apply(c.getNode()));
                        out!!.write("\" {")
                        indent++
                        body(tree, c)
                        indent--
                        newline()
                        out!!.write("}")
                    }
                    indent--
                    newline()
                    out!!.write("}")
                }
            }
        }
    }

    private fun newline() {
        out!!.write("\n")
        for (i in 0 until indent) {
            out!!.write("    ")
        }
    }

    private fun footer() {
        --indent
        newline()
        out!!.write("}\n")
    }

    companion object {
        var SEPARATOR = " // "

        var RANGE_SEPARATOR = " -- "

        var END_MARKER = "$$"


        fun getBranchingLabel(node: Node?): String {
            var n = node
            val sb = StringBuilder()
            while (n != null) {
                val p = n.parent()
                if (p != null && p.childrenCount() != 1) {
                    val branchLabel = n.nodeInfo.branchLabel
                    sb.append(if (branchLabel != null && !branchLabel.isEmpty())
                        branchLabel
                    else
                        "#" + p.getChildNr(n))
                            .append(SEPARATOR)
                }
                n = p
            }
            sb.append(END_MARKER)
            return sb.toString()
        }
    }
}
