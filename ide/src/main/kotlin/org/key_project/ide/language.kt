package org.key_project.ide

import de.uka.ilkd.key.nparser.KeYLexer
import de.uka.ilkd.key.nparser.KeYParser
import de.uka.ilkd.key.nparser.KeYParserBaseVisitor
import javafx.scene.Node
import javafx.scene.control.TreeItem
import org.antlr.v4.runtime.*
import java.nio.file.Path


abstract class Language {
    abstract val name: String
    abstract fun lexerFactory(input: CharStream): Lexer

    protected var ignore: Set<Int> = setOf()
    protected var structural: Set<Int> = setOf()
    protected var separators: Set<Int> = setOf()
    protected var literals: Set<Int> = setOf()
    protected var identifiers: Set<Int> = setOf()
    protected var specialIds: Set<Int> = setOf()
    protected var comments: Set<Int> = setOf()
    protected var docComments: Set<Int> = setOf()
    protected var datatype: Set<Int> = setOf()
    protected var control: Set<Int> = setOf()
    protected var operators: Set<Int> = setOf()
    protected var errorChar: Int = -2
    protected var nearlyInvisible: Set<Int> = setOf()

    open fun getStyleClass(tokenType: Int) =
        when (tokenType) {
            in separators -> "separator"
            in structural -> "structural"
            in literals -> "literal"
            in identifiers -> "identifier"
            in specialIds -> "fancy-identifier"
            in comments -> "comment"
            in docComments -> "doc-comment"
            in datatype -> "datatype"
            in control -> "control"
            in operators -> "operators"
            in ignore -> ""
            in nearlyInvisible -> "nearly-invisible"
            else -> {
                System.err.println("token type $tokenType (${javaClass.name}) is not registered for syntax highlighting.")
                ""
            }
        }

    abstract fun contextFactory(input: CharStream, errorListener: ANTLRErrorListener): ParserRuleContext
    abstract fun computeOutline(ctx: ParserRuleContext, editor: Editor): TreeItem<OutlineEntry>?
    abstract fun computeIssues(ctx: ParserRuleContext, filename: Path?, editor: Editor): List<IssueEntry>
}

internal class IssueErrorListener(val seq: MutableList<IssueEntry>) : ConsoleErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        val tok = offendingSymbol as Token?
        seq.add(IssueEntry(msg, "Parse Error", tok?.startIndex ?: 0))
        System.err.println("line $line:$charPositionInLine $msg")
    }
}

object KeyLanguage : Language() {
    override val name: String = "KeY"

    override fun lexerFactory(input: CharStream): Lexer = KeYLexer(input)
    override fun contextFactory(input: CharStream, errorListener: ANTLRErrorListener): ParserRuleContext =
        KeYParser(CommonTokenStream(KeYLexer(input))).let {
            it.addErrorListener(errorListener)
            it.file()
        }

    override fun computeOutline(ctx: ParserRuleContext, editor: Editor) =
        TreeItem(OutlineEntry(editor.title.value, editor, caretPosition = 0)).apply {
            ctx.accept(OutlineVisitorKey(this, editor))
        }

    override fun computeIssues(ctx: ParserRuleContext, filename: Path?, editor: Editor): List<IssueEntry> {
        val seq = arrayListOf<IssueEntry>()
        //FIXME load by key
        return seq;
    }

    init {
        structural = setOf(
            KeYLexer.SORTS, KeYLexer.FUNCTIONS, KeYLexer.PREDICATES, KeYLexer.TRANSFORMERS,
            KeYLexer.RULES, KeYLexer.JAVASOURCE, KeYLexer.CLASSPATH, KeYLexer.BOOTCLASSPATH, KeYLexer.CHOOSECONTRACT,
            KeYLexer.PROBLEM, KeYLexer.CONTRACTS, KeYLexer.AXIOMS
        )

        datatype = setOf()

        literals = setOf(
            KeYLexer.BIN_LITERAL, KeYLexer.HEX_LITERAL, KeYLexer.CHAR_LITERAL,
            KeYLexer.NUM_LITERAL, KeYLexer.QUOTED_STRING_LITERAL, KeYLexer.STRING_LITERAL
        )

        control = setOf()

        operators = setOf(
            KeYLexer.ADD, KeYLexer.ADDPROGVARS, KeYLexer.ADDRULES,
            KeYLexer.AND, KeYLexer.OR, KeYLexer.IMP, KeYLexer.NOT,
            KeYLexer.NOTFREEIN, KeYLexer.NOT_, KeYLexer.NOT_EQUALS, KeYLexer.EQUALS,
            KeYLexer.AT
        )

        separators = setOf(
            KeYLexer.DOT, KeYLexer.LPAREN, KeYLexer.RPAREN, KeYLexer.RBRACE, KeYLexer.RBRACE,
            KeYLexer.LBRACE, KeYLexer.LBRACKET
        )

        identifiers = setOf(KeYLexer.IDENT)
        comments = setOf(KeYLexer.ML_COMMENT, KeYLexer.SL_COMMENT)
        docComments = setOf(KeYLexer.DOC_COMMENT)
        errorChar = KeYLexer.ERROR_CHAR
    }
}

class OutlineVisitorKey(val root: TreeItem<OutlineEntry>, val editor: Editor) : KeYParserBaseVisitor<Unit>() {

    val sorts = createNode("Sorts")
    val functions = createNode("Functions")
    val choices = createNode("Choices")
    val predicates = createNode("Predicates")

    override fun visitDecls(ctx: KeYParser.DeclsContext?) {
        root.isExpanded = true
        root.children.addAll(sorts, functions, choices, predicates)
        super.visitDecls(ctx)
    }

    override fun visitFunc_decl(ctx: KeYParser.Func_declContext) {
        val text = "${ctx.func_name.text} : ${ctx.sortId().text}"
        val item = createNode(text, ctx)
        functions.children.add(item)
    }

    override fun visitPred_decl(ctx: KeYParser.Pred_declContext) {
        val item = createNode(ctx.funcpred_name().text, ctx.funcpred_name())
        predicates.children.add(item)
    }

    override fun visitOne_sort_decl(ctx: KeYParser.One_sort_declContext) {
        for (simpleIdentDot in ctx.sortIds.simple_ident_dots()) {
            sorts.children.add(createNode(simpleIdentDot.text, simpleIdentDot))
        }
    }

    override fun visitOptions_choice(ctx: KeYParser.Options_choiceContext?) {
        super.visitOptions_choice(ctx)
    }

    override fun visitRulesOrAxioms(ctx: KeYParser.RulesOrAxiomsContext) {
        addNode(
            if (null != ctx.AXIOMS()) "Axioms" else "Rules",
            ctx,
            children = ctx.taclet().map { createNode(it.name.text, it) })

    }

    override fun visitProblem(ctx: KeYParser.ProblemContext) {
        addNode("Problem", ctx)
    }

    private fun addNode(
        text: String,
        ctx: ParserRuleContext? = null,
        graphic: Node? = null,
        children: List<TreeItem<OutlineEntry>>? = null
    ): TreeItem<OutlineEntry> {
        return createNode(text, ctx, graphic, children).also { root.children.add(it) }
    }

    private fun createNode(
        text: String,
        ctx: ParserRuleContext? = null,
        graphic: Node? = null,
        children: List<TreeItem<OutlineEntry>>? = null
    ): TreeItem<OutlineEntry> {
        val oe = OutlineEntry(text, editor, ctx?.let { ctx.start?.startIndex })
        val t = TreeItem(oe)
        t.graphic = graphic
        if (children != null) t.children.setAll(children)
        t.isExpanded = true
        return t
    }

    override fun visitOneJavaSource(ctx: KeYParser.OneJavaSourceContext) {
        createNode("Java Source", ctx,
            children = ctx.string_value().map { createNode(it.text, ctx = it) })
            .also { root.children.add(it) }
    }
}

/*
object JavaLanguage : Language() {

    override val name: String = "Java+Jml"

    override fun lexerFactory(input: CharStream): Lexer = JavaJMLLexer(input)
    override fun contextFactory(input: CharStream, errorListener: ANTLRErrorListener): ParserRuleContext =
        JavaJMLParser(CommonTokenStream(JavaJMLLexer(input))).let {
            it.addErrorListener(errorListener)
            it.compilationUnit()
        }

    override fun computeOutline(ctx: ParserRuleContext, editor: Editor) =
        TreeItem(OutlineEntry(editor.title.value, editor, caretPosition = 0)).apply {
            ctx.accept(JavaOutlineVisitor(this, editor))
        }

    override fun computeIssues(ctx: ParserRuleContext, filename: Path?, editor: Editor): List<IssueEntry> {
        val seq = arrayListOf<IssueEntry>()
        if (filename != null && Files.exists(filename)) {
            seq.addAll(useJavaCompiler(filename))
        }
        return seq
    }


    private fun useJavaCompiler(filename: Path): Collection<IssueEntry> {
        val seq = arrayListOf<IssueEntry>()
        val javacListener = DiagnosticListener<JavaFileObject> { diagnostic ->
            logger.info {
                diagnostic.toString()
                if (diagnostic.kind == Diagnostic.Kind.WARNING || diagnostic.kind == Diagnostic.Kind.ERROR) {
                    seq += IssueEntry(
                        diagnostic.getMessage(Locale.ENGLISH) + "in ${diagnostic.source}:${diagnostic.lineNumber}:${diagnostic.columnNumber}",
                        "Java Compiler",
                        diagnostic.position.toInt()
                    )
                }
            }
        }

        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler != null) {
            logger.info { "Could not find javac for the current JVM." }
            val fileManager: StandardJavaFileManager = compiler.getStandardFileManager(javacListener, null, null)
            val units: Iterable<JavaFileObject?> =
                fileManager.getJavaFileObjectsFromFiles(arrayListOf(filename.toFile()))
            val task = compiler.getTask(null, fileManager, javacListener, null, null, units)
            task.call()
            fileManager.close()
        }
        return seq
    }

    init {
        structural = setOf(
            JavaJMLLexer.CLASS,
            JavaJMLLexer.IF,
            JavaJMLLexer.RETURN,
            JavaJMLLexer.ELSE,
            JavaJMLLexer.IMPLEMENTS,
            JavaJMLLexer.EXTENDS,
            JavaJMLLexer.WHILE,
            JavaJMLLexer.FOR,
            JavaJMLLexer.INTERFACE,
            JavaJMLLexer.PUBLIC,
            JavaJMLLexer.PRIVATE,
            JavaJMLLexer.PROTECTED,
            JavaJMLLexer.PURE,
            JavaJMLLexer.STRICTLY_PURE,
            JavaJMLLexer.NULLABLE,
            JavaJMLLexer.NULLABLE_BY_DEFAULT,
            JavaJMLLexer.NON_NULL,
            JavaJMLLexer.LBLPOS,
            JavaJMLLexer.LBLNEG,
            JavaJMLLexer.FORALL,
            JavaJMLLexer.EXISTS,
            JavaJMLLexer.BY,
            JavaJMLLexer.DECLASSIFIES,
            JavaJMLLexer.ERASES,
            JavaJMLLexer.NEW_OBJECTS,
            JavaJMLLexer.ASSIGNABLE,
            JavaJMLLexer.ACCESSIBLE,
            JavaJMLLexer.ENSURES,
            JavaJMLLexer.REQUIRES,
            JavaJMLLexer.SIGNALS,
            JavaJMLLexer.SIGNALS_ONLY,
            JavaJMLLexer.NORMAL_BEHAVIOR,
            JavaJMLLexer.BEHAVIOR,
            JavaJMLLexer.EXCEPTIONAL_BEHAVIOR,
            JavaJMLLexer.STRICTLY_PURE,
            JavaJMLLexer.PURE,
            JavaJMLLexer.DO,
            JavaJMLLexer.LOOP_DETERMINES,
            JavaJMLLexer.LOOP_INVARIANT,
            JavaJMLLexer.LOOP_SEPARATES,
            JavaJMLLexer.DECLASSIFIES,
            JavaJMLLexer.DECREASES,
            JavaJMLLexer.INVARIANT,
            JavaJMLLexer.REPRESENTS,
            JavaJMLLexer.UNREACHABLE,
            JavaJMLLexer.ASSERT,
            JavaJMLLexer.DIVERGES,
            JavaJMLLexer.INSTANCE,
            JavaJMLLexer.MODEL,
            JavaJMLLexer.MODEL_METHOD_AXIOM,
            JavaJMLLexer.ALSO,
        )

        operators = setOf(
            JavaJMLLexer.INSTANCEOF,
            JavaJMLLexer.ADD,
            JavaJMLLexer.ADD_ASSIGN,
            JavaJMLLexer.SUB_ASSIGN,
            JavaJMLLexer.SUB,
            JavaJMLLexer.DIV,
            JavaJMLLexer.DIV_ASSIGN,
            JavaJMLLexer.MUL,
            JavaJMLLexer.MUL_ASSIGN,
            JavaJMLLexer.MOD,
            JavaJMLLexer.MOD_ASSIGN,
            JavaJMLLexer.ST,
            JavaJMLLexer.XOR_ASSIGN,
            JavaJMLLexer.LT,
            JavaJMLLexer.GT,
            JavaJMLLexer.LSHIFT_ASSIGN,
            JavaJMLLexer.RSHIFT_ASSIGN,
            JavaJMLLexer.URSHIFT_ASSIGN,
            JavaJMLLexer.AND_ASSIGN,
            JavaJMLLexer.OR_ASSIGN,
            JavaJMLLexer.OR,
            JavaJMLLexer.AND,
            JavaJMLLexer.IMPLIES,
            JavaJMLLexer.IMPLIESBACKWARD,
            JavaJMLLexer.EQUAL,
            JavaJMLLexer.NOTEQUAL,
            JavaJMLLexer.EQUIVALENCE,
        )

        datatype = setOf(
            JavaJMLLexer.INT,
            JavaJMLLexer.BOOLEAN,
            JavaJMLLexer.FLOAT,
            JavaJMLLexer.DOUBLE,
            JavaJMLLexer.VOID,
        )

        separators = setOf(
            JavaJMLLexer.RBRACE,
            JavaJMLLexer.LBRACE,
            JavaJMLLexer.RPAREN,
            JavaJMLLexer.LPAREN,
            JavaJMLLexer.LBRACK,
            JavaJMLLexer.RBRACK,
            JavaJMLLexer.COLON,
            JavaJMLLexer.COMMA,
            JavaJMLLexer.SEMI,
            JavaJMLLexer.SEMI_TOPLEVEL,
            JavaJMLLexer.DOT,
            JavaJMLLexer.DOTDOT,
            JavaJMLLexer.JML_START,
            JavaJMLLexer.JML_COMMENT_END,
            JavaJMLLexer.JML_END,
        )
        literals = setOf(
            JavaJMLLexer.BINARY_LITERAL,
            JavaJMLLexer.BINARY_LITERAL,
            JavaJMLLexer.FLOAT_LITERAL,
            JavaJMLLexer.HEX_FLOAT_LITERAL,
            JavaJMLLexer.BOOL_LITERAL,
            JavaJMLLexer.CHAR_LITERAL,
            JavaJMLLexer.STRING_LITERAL,
            JavaJMLLexer.NULL_LITERAL
        )
        identifiers = setOf(JavaJMLLexer.IDENTIFIER)
        comments = setOf(JavaJMLLexer.LINE_COMMENT, JavaJMLLexer.COMMENT_START)
        nearlyInvisible = setOf(JavaJMLLexer.WS_CONTRACT, JavaJMLLexer.WS_CONTRACT_IGNORE)
        ignore = setOf(JavaJMLLexer.WS, JavaJMLLexer.WS_CONTRACT_IGNORE)
    }
}

class JavaOutlineVisitor(val cu: TreeItem<OutlineEntry>, val editor: Editor) : JavaJMLParserBaseVisitor<Unit>() {
    var parent: TreeItem<OutlineEntry> = cu

    override fun visitClassDeclaration(ctx: JavaJMLParser.ClassDeclarationContext) {
        makeParent("Class ${ctx.IDENTIFIER().text}", ctx) {
            super.visitClassDeclaration(ctx)
        }
    }

    override fun visitEnumDeclaration(ctx: JavaJMLParser.EnumDeclarationContext) {
        makeParent("Enum ${ctx.IDENTIFIER().text}", ctx) {
            super.visitEnumDeclaration(ctx)
        }
    }

    override fun visitInterfaceDeclaration(ctx: JavaJMLParser.InterfaceDeclarationContext) {
        makeParent("Enum ${ctx.IDENTIFIER().text}", ctx) {
            super.visitInterfaceDeclaration(ctx)
        }
    }

    override fun visitMethodDeclaration(ctx: JavaJMLParser.MethodDeclarationContext) {
        makeParent("${ctx.IDENTIFIER().text}${ctx.formalParameters().text} : ${ctx.typeTypeOrVoid().text}") {
            super.visitMethodDeclaration(ctx)
        }
    }

    override fun visitFieldDeclaration(ctx: JavaJMLParser.FieldDeclarationContext) {
        addNode("${ctx.variableDeclarators().text} : ${ctx.typeType().text}")
    }

    override fun visitMethodContract(ctx: JavaJMLParser.MethodContractContext?) {
        addNode("Method-Contract", ctx)
    }

    override fun visitClassInv(ctx: JavaJMLParser.ClassInvContext) {
        addNode("Invariant: ${ctx.expr().text}", ctx)
    }

    override fun visitFieldDecl(ctx: JavaJMLParser.FieldDeclContext) {
        addNode("${ctx.id()} ${ctx.typeType()}", ctx)
    }

    override fun visitRepresents(ctx: JavaJMLParser.RepresentsContext) {
        addNode("Represents", ctx)
    }

    override fun visitAccessible(ctx: JavaJMLParser.AccessibleContext?) {
        addNode("Accessible", ctx)
    }

    override fun visitVisibility(ctx: JavaJMLParser.VisibilityContext?) {
        addNode("Visibility", ctx)
    }


    private fun makeParent(
        text: String,
        ctx: ParserRuleContext? = null,
        graphic: Node? = null,
        children: List<TreeItem<OutlineEntry>>? = null,
        block: () -> Unit
    ) {
        makeParent(addNode(text, ctx, graphic, children), block)
    }

    private fun makeParent(node: TreeItem<OutlineEntry>, ctx: () -> Unit) {
        val backup = parent
        parent = node
        ctx()
        parent = backup
    }

    private fun addNode(
        text: String,
        ctx: ParserRuleContext? = null,
        graphic: Node? = null,
        children: List<TreeItem<OutlineEntry>>? = null
    ): TreeItem<OutlineEntry> {
        return createNode(text, ctx, graphic, children).also { parent.children.add(it) }
    }

    private fun createNode(
        text: String,
        ctx: ParserRuleContext? = null,
        graphic: Node? = null,
        children: List<TreeItem<OutlineEntry>>? = null
    ): TreeItem<OutlineEntry> {
        val oe = OutlineEntry(text, editor, ctx?.let { ctx.start?.startIndex })
        val t = TreeItem(oe)
        t.graphic = graphic
        if (children != null) t.children.setAll(children)
        t.isExpanded = true
        return t
    }
}

*/