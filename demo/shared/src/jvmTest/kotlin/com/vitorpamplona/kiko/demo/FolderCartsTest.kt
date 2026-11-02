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

import com.vitorpamplona.kiko.player.KikoPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class FolderCartsTest {
    private val root = createTempDirectory("folder-carts").toFile()
    private val folder = File(root, "game").apply { mkdir() }
    private val carts = FolderCarts(folder)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(
        file: File,
        text: String = file.name,
    ) = file.writeText(text)

    private fun loaded(name: String) = carts.loadCart(name)?.decodeToString()

    @Test
    fun anIdOnTheOriginalsSiteIsItsPngBesideTheOpenedCart() {
        write(File(folder, "intoruins_main-7.p8.png"))
        assertEquals("intoruins_main-7.p8.png", loaded("#intoruins_main-7"))
    }

    @Test
    fun aFileNameIsTakenAsTheCartWroteIt() {
        write(File(folder, "level2.p8"), "the file")
        write(File(folder, "level2.p8.p8"), "not this one")
        assertEquals("the file", loaded("level2.p8"))
    }

    @Test
    fun aNameWithoutAnExtensionFindsTheCart() {
        write(File(folder, "level3.p8"))
        assertEquals("level3.p8", loaded("level3"))
    }

    @Test
    fun anIdWithoutAVersionIsItsLatestVersion() {
        write(File(folder, "game-2.p8.png"), "old")
        write(File(folder, "game-10.p8.png"), "new")
        write(File(folder, "game-other.p8.png"))
        write(File(folder, "gamey-30.p8.png"))
        write(File(folder, "30.p8.png"))
        assertEquals("new", loaded("#game"))
    }

    @Test
    fun anIdThatMatchesExactlyBeatsItsVersions() {
        write(File(folder, "game.p8.png"), "exact")
        write(File(folder, "game-2.p8.png"), "versioned")
        assertEquals("exact", loaded("#game"))
    }

    @Test
    fun onlyAnIdMeansItsLatestVersion() {
        write(File(folder, "game-2.p8.png"))
        assertNull(loaded("game"))
    }

    @Test
    fun aCartThatIsNotThereIsNull() {
        assertNull(loaded("#elsewhere"))
    }

    @Test
    fun aFolderIsNotACart() {
        File(folder, "levels.p8").mkdir()
        assertNull(loaded("levels"))
    }

    @Test
    fun aNameNeverLeavesTheFolder() {
        val secret = File(root, "secret.p8").apply { writeText("secret") }
        File(folder, "sub").mkdir()
        write(File(folder, "sub/inner.p8"))
        for (name in listOf("../secret.p8", "../secret", "#../secret", "..\\secret.p8", secret.absolutePath, "sub/inner.p8")) {
            assertNull(loaded(name), name)
        }
    }

    @Test
    fun aGameContinuesInTheCartBesideIt() {
        write(File(folder, "part2.p8.png"), "pico-8 cartridge\n__lua__\nstop()\n")
        val front = "pico-8 cartridge\n__lua__\nload(\"#part2\")\n".encodeToByteArray()
        KikoPlayer(source = { front }, host = carts, openAudio = { null }).use { player ->
            player.resume()
            val end = runBlocking { withTimeout(30.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } } }
            // Without the second cart the game stops with an error saying so.
            assertEquals(KikoPlayer.Status.Stopped(error = null), end)
        }
    }
}
