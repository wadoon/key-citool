import de.uka.ilkd.key.nparser.ParsingFacade
import org.junit.jupiter.api.Test
import org.key_project.core.doc.*
import java.io.File

/**
 *
 * @author Alexander Weigl
 * @version 1 (1/26/22)
 */

class PPrintTest {
    @Test
    fun first() {
        val d =
            string("begin") `^^` nest(
                4, break1 `^^` string("stmt;")
                        `^^` break1 `^^` string("stmt;")
                        `^^` break1 `^^` string("stmt;")
            ) `^^` break1 `^^` string("end");
        println(pretty(d, 40))
    }

    @Test
    fun mapFile() {
        val ast = ParsingFacade.parseFile(File("src/test/resources/map.key"))
        println("read!")
        val txt = pretty(
            ParsingFacade.getParseRuleContext(ast),
            Index(),
            Symbol("Test", "xxx", type = Symbol.Type.FILE),
            hashMapOf()
        )
        println(txt)
    }
}