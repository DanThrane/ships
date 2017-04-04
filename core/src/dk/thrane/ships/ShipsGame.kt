package dk.thrane.ships

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import java.io.BufferedReader
import java.io.InputStreamReader
import com.badlogic.gdx.Gdx.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.MathUtils

class SpriteSheet(
        resource: String,
        val widthInPixels: Int,
        val heightInPixels: Int,
        paddingX: Int = 0,
        paddingY: Int = 0,
        val widthInDestination: Float,
        val heightInDestination: Float
) {
    private val texture: Texture = Texture(resource)
    val columns = texture.width / (widthInPixels + paddingX)
    val rows = texture.height / (heightInPixels + paddingY)

    val sprites = Array(columns * rows) { i ->
        val x = i % columns
        val y = i / columns

        val result = Sprite(
                TextureRegion(
                        texture,
                        x * (widthInPixels + paddingX), y * (heightInPixels + paddingY),
                        widthInPixels, heightInPixels
                )
        )
        result.setSize(widthInDestination, heightInDestination)
        result.setOriginCenter()
        return@Array result
    }
}

fun asciiMapping(map: Map<Char, Int>): (Char) -> Int = { map[it]!! }

class AsciiMap(
        fileName: String,
        private val sheet: SpriteSheet,
        val tileWidth: Float,
        val tileHeight: Float,
        asciiToTile: (Char) -> Int
) {
    private val map: IntArray
    private val width: Int
    private val height: Int

    init {
        val reader = BufferedReader(InputStreamReader(Gdx.files.internal(fileName).read(), Charsets.US_ASCII))
        val dimensions = reader.readLine().split(" ").map(String::toInt)
        width = dimensions[0]
        height = dimensions[0]
        assert(dimensions.size == 2) { "Expected exactly two dimensions" }
        map = IntArray(dimensions[0] * dimensions[1])
        var i = 0
        reader.forEachLine { line ->
            assert(i < map.size) { "Too many rows!" }
            assert(line.length == dimensions[0]) {
                "Expected line to be of length ${dimensions[0]}, but was of ${line.length}"
            }

            line.forEach {
                map[i++] = asciiToTile(it)
            }
        }
    }

    fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        // For now let's completely ignore the camera and just the render the entire thing.
        // TODO Ideally we should only be rendering the stuff that can be on the game screen.
        // Gotta save those precious cycles.

        var i = 0
        for (y in (0..height - 1)) {
            for (x in (0..width - 1)) {
                val tileIdx = map[i++]
                if (tileIdx != -1) {
                    val sprite = sheet.sprites[tileIdx]
                    sprite.setPosition(x * tileWidth, y * tileHeight)
                    sprite.draw(batch)
                }
            }
        }
    }
}

class Ship {
    var position = Vector2(0f, 0f)
    var speed = Vector2(0f, 0f)
    lateinit var sprite: Sprite

    fun init() {
        sprite = Sprite(Texture("ship.png"))
        sprite.setPosition(0f, 0f)
        sprite.setSize(1f, 0.587f)
        sprite.setOriginCenter()
    }

    fun render(batch: SpriteBatch) {
        val angle = speed.y * MathUtils.radiansToDegrees

        sprite.rotation = angle
        sprite.setPosition(position.x, position.y)
        sprite.draw(batch)
    }
}

class Projectile {
    val position = Vector2(0f, 0f)
    val speed = Vector2(0f, 0f)
    var active = false
    var lifeRemaining = 0f
    lateinit var sprite: Sprite

    fun update(dt: Float) {
        if (active) {
            position.mulAdd(speed, dt)

            lifeRemaining -= dt
            if (lifeRemaining < 0) {
                active = false
            }
        }
    }

    fun render(batch: SpriteBatch) {
        if (active) {
            sprite.setPosition(position.x, position.y)
            sprite.draw(batch)
        }
    }
}

class ShipsGame : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var uiBatch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var ship: Ship
    lateinit var camera: OrthographicCamera
    lateinit var sheet: SpriteSheet
    lateinit var map: AsciiMap
    lateinit var background: AsciiMap
    lateinit var cannonball: Projectile
    var isShooting = false
    var isRebounding = false
    var power = 0f

    override fun create() {
        batch = SpriteBatch()
        uiBatch = SpriteBatch()
        font = BitmapFont()
        ship = Ship()
        cannonball = Projectile()
        cannonball.sprite = Sprite(Texture("cannonBall.png"))
        cannonball.sprite.setSize(0.25f, 0.25f)
        cannonball.sprite.setOriginCenter()

        val h = Gdx.graphics.height
        val w = Gdx.graphics.width
        camera = OrthographicCamera(32f, 32f * (h / w))
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0f)
        camera.update()

        sheet = SpriteSheet("tiles_sheet.png", 64, 64, widthInDestination = 1.01f, heightInDestination = 1.01f)
        val mapping = asciiMapping(mapOf(
                'Q' to 0,
                'L' to 16,
                'S' to 67,
                'R' to 18,
                'W' to 72,
                '_' to -1
        ))
        map = AsciiMap("map", sheet, 1f, 1f, mapping)
        background = AsciiMap("background", sheet, 1f, 1f, mapping)

        ship.init()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = 30f
        camera.viewportHeight = 30f * height / width
        camera.update()
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime
        update(dt)
        camera.update()
        batch.projectionMatrix = camera.combined

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)

        batch.begin()
        background.render(batch, camera)
        map.render(batch, camera)
        ship.render(batch)
        cannonball.render(batch)
        batch.end()

        uiBatch.begin()
        font.draw(uiBatch, "FPS ${Gdx.graphics.framesPerSecond}", 10f, 440f)
        font.draw(uiBatch, "Position: ${ship.position.x}, ${ship.position.y}", 10f, 420f)
        font.draw(uiBatch, "Speed: ${ship.speed}", 10f, 400f)
        font.draw(uiBatch, "Speed (Cart): ${ship.speed.polarToCartesian()}", 10f, 380f)
        uiBatch.end()
    }

    private fun update(dt: Float) {
        // --- Handle input ---

        // Movement
        if (input.isKeyPressed(Input.Keys.W)) ship.speed.x += dt * 10
        if (input.isKeyPressed(Input.Keys.S)) ship.speed.x -= dt * 10
        if (ship.speed.x < 0) ship.speed.x = 0f
        if (ship.speed.x > 3) ship.speed.x = 3f
        if (input.isKeyPressed(Input.Keys.D)) ship.speed.y -= dt * 1
        if (input.isKeyPressed(Input.Keys.A)) ship.speed.y += dt * 1

        // Actions
        if (!isRebounding && input.isKeyPressed(Input.Keys.SPACE)) {
            power += 0.2f * dt
            power = Math.min(power, 0.5f)
            camera.zoom = 1 - power
            isShooting = true
        } else if (isShooting) {
            cannonball.position.x = ship.position.x
            cannonball.position.y = ship.position.y

            cannonball.speed.set(power * 30f, ship.speed.y).setPolarToCartesian()
            cannonball.lifeRemaining = 3f
            cannonball.active = true

            power = 0f
            isShooting = false
            isRebounding = true
        }

        // --- End input ---

        // --- Game logic ---

        if (isRebounding) {
            camera.zoom += 0.4f * dt
            if (camera.zoom >= 1f) {
                camera.zoom = 1f
                isRebounding = false
            }
        }
        ship.position.mulAdd(ship.speed.polarToCartesian(), dt)
        camera.position.x = ship.position.x
        camera.position.y = ship.position.y
        cannonball.update(dt)

        // --- End logic ---
    }

    override fun dispose() {
        // TODO Do we really care?
    }
}

fun Vector2.setPolarToCartesian() {
    val oldX = x
    val oldY = y
    x = oldX * MathUtils.cos(oldY)
    y = oldX * MathUtils.sin(oldY)
}

fun Vector2.polarToCartesian(): Vector2 {
    val result = Vector2(this)
    result.x = this.x * MathUtils.cos(this.y)
    result.y = this.x * MathUtils.sin(this.y)
    return result
}

