/* key-tools are extension for the KeY theorem prover.
 * Copyright (C) 2021  Alexander Weigl
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the complete terms of the GNU General Public License, please see this URL:
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.key_project.core.doc

import de.uka.ilkd.key.nparser.KeYParser
import de.uka.ilkd.key.nparser.KeYParserBaseVisitor
import java.util.concurrent.TimeUnit

fun execute(vararg args: String): String {
    // App.putln(args.joinToString(" "))
    val pb = ProcessBuilder(args.toList())
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
    val p = pb.start()
    p.waitFor(1, TimeUnit.SECONDS)
    return p.inputStream.reader().readText()
}

val GIT_VERSION by lazy {
    execute("git", "describe", "--all")
}

class Indexer(val self: String, val index: Index) : KeYParserBaseVisitor<Unit>() {
    override fun visitFile(ctx: KeYParser.FileContext) {
        index += Symbol.file(self, ctx)
        super.visitFile(ctx)
    }

    override fun visitOne_sort_decl(ctx: KeYParser.One_sort_declContext) {
        for (name in ctx.sortIds.simple_ident_dots()) {
            index += Symbol.sort(self, name.text, name)
        }
    }

    override fun visitFunc_decl(ctx: KeYParser.Func_declContext) {
        index += Symbol.function(self, ctx.func_name.text, ctx)
    }

    override fun visitTransform_decl(ctx: KeYParser.Transform_declContext) {
        index += Symbol.transformer(self, ctx.trans_name.text, ctx)
    }

    override fun visitPred_decl(ctx: KeYParser.Pred_declContext) {
        index += Symbol.predicate(self, ctx.pred_name.text, ctx)
    }

    override fun visitTaclet(ctx: KeYParser.TacletContext) {
        index += Symbol.taclet(self, ctx.name.text, ctx)
    }

    override fun visitChoice(ctx: KeYParser.ChoiceContext) {
        index += Symbol.choiceCategory(self, ctx.category.text)
        ctx.optionDecl().forEach { co ->
            index += Symbol.choiceOption(self, ctx.category.text, co.IDENT.text, co)
        }
    }

    override fun visitRuleset_decls(ctx: KeYParser.Ruleset_declsContext) {
        ctx.id.forEach {
            index += Symbol.ruleset(it.text, self, it)
        }
    }

    override fun visitOne_contract(ctx: KeYParser.One_contractContext) {
        index += Symbol.contract(ctx.contractName.text, self, ctx)
    }

    override fun visitOne_invariant(ctx: KeYParser.One_invariantContext) {
        index += Symbol.invariant(ctx.invName.text, self, ctx)
    }
}

/**
 * Represents a link to an entry.
 */
open class Symbol(
    val displayName: String,
    val url: String,
    val target: String = displayName,
    val type: Type,
    val ctx: Any? = null
) {
    open val anchor = "$type-$target"
    open val href = "$url#$anchor"

    enum class Type(val navigationTitle: String) {
        CATEGORY("Choice categories"),
        OPTION("Choice options"),
        SORT("Sorts"),
        PREDICATE("Predicates"),
        FUNCTION("Functions"),
        TRANSFORMER("Transformers"),
        RULESET("Rulesets"),
        TACLET("Taclets"),
        CONTRACT("Contracts"),
        INVARIANT("Invariants"),
        FILE("Files"),
        TOKEN("t"), EXTERNAL("ext");
    }

    companion object {
        fun choiceCategory(page: String, cat: String, ctx: Any? = null): Symbol =
            Symbol(cat, page, cat, Type.CATEGORY, ctx)

        fun choiceOption(page: String, cat: String, option: String, ctx: Any? = null): Symbol =
            Symbol("$cat:$option", page, "$cat-$option", Type.OPTION, ctx)

        fun taclet(page: String, text: String, ctx: Any? = null) = Symbol(text, page, text, Type.TACLET, ctx)
        fun predicate(page: String, text: String, ctx: Any? = null) = Symbol(text, page, text, Type.PREDICATE, ctx)
        fun function(page: String, text: String, ctx: Any? = null) = Symbol(text, page, text, Type.FUNCTION, ctx)
        fun sort(page: String, text: String, ctx: Any? = null) = Symbol(text, page, type = Type.SORT, ctx = ctx)
        fun transformer(page: String, text: String, ctx: Any? = null) = Symbol(text, page, text, Type.TRANSFORMER, ctx)
        fun file(self: String, ctx: Any? = null) = Symbol(self.replace(".html", ""), self, "root", Type.FILE, ctx)
        fun ruleset(name: String, page: String, ctx: Any? = null) = Symbol(name, page, name, Type.RULESET, ctx)
        fun token(display: String, tokenType: Int) = TokenSymbol(display, tokenType)
        fun external(url: String, anchor: String = "", ctx: Any? = null) =
            object : Symbol("", url, "", Type.EXTERNAL, ctx) {
                override val anchor: String = anchor
                override val href = url
            }

        fun contract(name: String, self: String, ctx: Any? = null) = Symbol(name, self, name, Type.CONTRACT, ctx)
        fun invariant(name: String, self: String, ctx: Any? = null) = Symbol(name, self, name, Type.INVARIANT, ctx)
    }

    override fun toString(): String {
        return "Symbol(displayName='$displayName', url='$url', target='$target', type=$type, ctx=$ctx, anchor='$anchor', href='$href')"
    }
}

data class TokenSymbol(val display: String, val tokenType: Int) :
    Symbol(display, "https://key-project.org/docs/grammar/", display, Type.TOKEN)

typealias Index = ArrayList<Symbol>
typealias Usages = MutableList<Symbol>
typealias UsageIndex = MutableMap<Symbol, Usages>

fun UsageIndex.add(used: Symbol, where: Symbol) = computeIfAbsent(used) { it -> ArrayList(1024) }.add(where)
