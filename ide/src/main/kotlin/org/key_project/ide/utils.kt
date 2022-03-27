package org.key_project.ide

import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

/**
 *
 */
internal val logger = LoggerFactory.getLogger("key-ide")

/**
 * A little of dependency injection.
 */
class Context {
    private val map = HashMap<Class<*>, Any>()
    inline fun <reified T : Any> register(obj: T) = register(obj, T::class.java)
    fun <T : Any> register(obj: T, clazz: Class<T>) = obj.also { map[clazz] = it }
    inline fun <reified T : Any> get(): T = get(T::class.java)
    fun <T : Any> get(clazz: Class<T>): T = map[clazz] as T

    interface ReadOnlyProp<T> {
        operator fun getValue(self: Any, property: KProperty<*>): T
    }

    inline fun <reified T : Any> ref() = object : ReadOnlyProp<T> {
        override operator fun getValue(self: Any, property: KProperty<*>): T {
            return get(T::class.java)
        }
    }
}

