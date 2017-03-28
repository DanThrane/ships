package dk.thrane.game.util

import com.badlogic.gdx.utils.Pool
import java.util.*
import kotlin.reflect.KClass

abstract class Component<T> : Pool.Poolable where T : Component<T> {
    abstract val descriptor: ComponentDescriptor<T>

    fun free() {
        World.freeComponent(descriptor.typeIndex, this)
    }
}

abstract class ComponentDescriptor<T> where T : Component<T> {
    private var _typeIndex: Int = -1
    val typeIndex get() = _typeIndex

    abstract protected fun createObject(): T

    fun create() = World.createComponent<T>(_typeIndex)

    fun init() {
        _typeIndex = World.register(this::createObject)
    }
}

class Entity : Pool.Poolable {
    val components = Array<Component<*>?>(64) { null }

    fun hasAllOf(vararg ids: Int) =
            ids.all { components[it] != null }

    fun hasAnyOf(vararg ids: Int) =
            ids.any { components[it] != null }

    fun addComponent(component: Component<*>) {
        val typeIndex = component.descriptor.typeIndex
        assert(components[typeIndex] == null)

        components[typeIndex] = component
    }

    fun removeComponent(typeIndex: Int) {
        components[typeIndex] = null
    }

    override fun reset() {
        Arrays.fill(components, null)
    }

    fun free() {
        TODO()
    }
}

interface System {
    fun update(dt: Float, entities: List<Entity>)
}

object World {
    private var nextComponent = 0
    private val systems = ArrayList<System>()
    private val componentPools = Array<Pool<Any>?>(64) { null }
    private val entities = ArrayList<Entity>()
    fun <T : Component<T>> register(constructor: () -> T): Int {
        componentPools[nextComponent] = object : Pool<Any>() {
            override fun newObject(): T = constructor()
        }
        return nextComponent++
    }

    fun createEntity(): Entity {
        val result = Entity()
        entities.add(result)
        return result
    }

    fun <T : Component<T>> createComponent(type: Int): T {
        assert(type >= 0)
        assert(type < nextComponent)

        @Suppress("UNCHECKED_CAST")
        return componentPools[type]!!.obtain() as T
    }

    fun freeComponent(type: Int, component: Component<*>) {
        assert(type >= 0)
        assert(type < nextComponent)

        componentPools[type]!!.free(component)
    }

    fun register(system: System) {
        systems.add(system)
    }

    fun update(dt: Float) {
        systems.forEach { it.update(dt, entities) }
    }
}
