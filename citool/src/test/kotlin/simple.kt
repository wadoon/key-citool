import de.uka.ilkd.key.obj2json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 *
 * @author Alexander Weigl
 * @version 1 (5/20/20)
 */

class Cli {
    @Test
    fun test_any2json() {
        assertEquals("1", obj2json(1))
        assertEquals("null", obj2json(null))
        assertEquals("\"abc\"", obj2json("abc"))
        assertEquals("1.2", obj2json(1.2))
        assertEquals("2.0E-9", obj2json(0.000000002))
        assertEquals("[1,2,3,4,5]", obj2json(arrayListOf(1, 2, 3, 4, 5)))
        assertEquals("[\"a\",\"b\",\"c\",4,5]", obj2json(arrayListOf("a", "b", "c", 4, 5)))
        assertEquals("{\"abc\" : 2}", obj2json(hashMapOf("abc" to 2)))
        assertEquals("{\"abc\" : {\"abc\" : 2}}", obj2json(hashMapOf("abc" to hashMapOf("abc" to 2))))
        assertEquals("{\"abc\" : 2}", obj2json(hashMapOf("abc" to 2)))
    }


    @Test
    fun empty() {
    }
}