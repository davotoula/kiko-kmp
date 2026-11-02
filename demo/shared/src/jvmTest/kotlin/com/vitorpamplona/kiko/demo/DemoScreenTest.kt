/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.kiko.demo

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** [DemoScreen] off screen, with a real player on its own thread. */
class DemoScreenTest {
    private val folder = createTempDirectory("demo-screen").toFile()
    private lateinit var scene: ImageComposeScene

    @AfterTest
    fun cleanUp() {
        scene.close()
        folder.deleteRecursively()
    }

    private fun Image.shows(color: Int): Boolean {
        val pixels = Bitmap.makeFromImage(this)
        return (0 until width step 8).any { x -> (0 until height step 8).any { y -> pixels.getColor(x, y) == color } }
    }

    private fun rendersWithin10Seconds(color: Int): Boolean {
        val deadline = System.nanoTime() + 10_000_000_000L
        var nanos = 0L
        while (System.nanoTime() < deadline) {
            nanos += 16_000_000
            if (scene.render(nanos).shows(color)) return true
            Thread.sleep(20)
        }
        return false
    }

    @OptIn(InternalComposeUiApi::class)
    @Test
    fun anOpenedCartContinuesInTheCartBesideIt() {
        // Green, because the message of a game that cannot continue is drawn in red.
        File(folder, "part2.p8").writeText("__lua__\nfunction _draw() cls(11) end\n")
        val front = "__lua__\nload(\"#part2\")\n".encodeToByteArray()
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                DemoScreen(opened = front, onOpen = null, openedHost = FolderCarts(folder))
            }
        assertTrue(rendersWithin10Seconds(GREEN), "the second cart never reached the screen")
    }

    private companion object {
        const val GREEN = 0xFF00E436.toInt()
    }
}
