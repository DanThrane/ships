package dk.thrane.ships.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import dk.thrane.ships.ShipsGame

object DesktopLauncher {
    @JvmStatic fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.width = 800
        config.height = 450
        LwjglApplication(ShipsGame(), config)
    }
}
