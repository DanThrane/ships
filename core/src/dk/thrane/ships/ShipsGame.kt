package dk.thrane.ships

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import dk.thrane.game.util.*

object Ship {
    lateinit var entity: Entity
    lateinit var position: PositionComponent
    lateinit var speed: MovementComponent
    lateinit var texture: Texture

    fun init() {
        entity = World.createEntity()
        position = PositionDescriptor.create()
        speed = MovementDescriptor.create()

        entity.addComponent(position)
        entity.addComponent(speed)

        position.x = 30f
        position.y = 30f

        texture = Texture("ship.png")
    }

    fun render(batch: SpriteBatch) {
        val angle = speed.vec.angle()
        val width = texture.width
        val height = texture.height

        batch.draw(
                texture,
                position.x, position.y,
                width / 2f, height / 2f,
                width.toFloat(), height.toFloat(),
                1f, 1f,
                angle,
                0, 0,
                width, height,
                false, false
        )
    }
}

class ShipsGame : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()

        PositionDescriptor.init()
        MovementDescriptor.init()
        World.register(MovementSystem)

        Ship.init()
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime
        handleInput(dt)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)

        World.update(dt)
        batch.begin()
        font.draw(batch, "FPS ${Gdx.graphics.framesPerSecond}", 10f, 440f)
        Ship.render(batch)
        batch.end()
    }

    private fun handleInput(dt: Float) {
        val isUpPressed = Gdx.input.isKeyPressed(Input.Keys.W)
        val isLeftPressed = Gdx.input.isKeyPressed(Input.Keys.A)
        val isRightPressed = Gdx.input.isKeyPressed(Input.Keys.D)
        val isDownPressed = Gdx.input.isKeyPressed(Input.Keys.S)

        if (isUpPressed) Ship.speed.vec.y += dt * 1000
        if (isDownPressed) Ship.speed.vec.y -= dt * 1000

        if (isRightPressed) Ship.speed.vec.x += dt * 1000
        if (isLeftPressed) Ship.speed.vec.x -= dt * 1000
    }

    override fun dispose() {
        // TODO Do we really care?
    }
}

object PositionDescriptor : ComponentDescriptor<PositionComponent>() {
    override fun createObject() = PositionComponent()
}

class PositionComponent : Component<PositionComponent>() {
    override val descriptor = PositionDescriptor
    var x: Float = 0f
    var y: Float = 0f

    override fun reset() {
        x = 0f
        y = 0f
    }

}

val Entity.position: PositionComponent
    get() = components[PositionDescriptor.typeIndex]!! as PositionComponent

object MovementDescriptor : ComponentDescriptor<MovementComponent>() {
    override fun createObject() = MovementComponent()
}

class MovementComponent : Component<MovementComponent>() {
    override val descriptor = MovementDescriptor

    val vec: Vector2 = Vector2(0f, 0f)

    override fun reset() {
        vec.x = 0f
        vec.y = 0f
    }
}

val Entity.movement: MovementComponent
    get() = components[MovementDescriptor.typeIndex]!! as MovementComponent

object MovementSystem : System {
    override fun update(dt: Float, entities: List<Entity>) {
        entities.filter {
            it.hasAllOf(PositionDescriptor.typeIndex, MovementDescriptor.typeIndex)
        }.forEach { entity ->
            val position = entity.position
            val movement = entity.movement
            position.x += movement.vec.x * dt
            position.y += movement.vec.y * dt
        }
    }
}

