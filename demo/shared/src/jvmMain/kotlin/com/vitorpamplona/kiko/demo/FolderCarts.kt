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

import com.vitorpamplona.kiko.player.KikoPlayerHost
import java.io.File

/**
 * Finds the other carts of a game in the [folder] of the one that was opened, so a game in several
 * carts plays from files: `#some_id` is `some_id.p8.png` there, or its latest `some_id-7.p8.png`
 * when the cart names no version; `level2` is `level2.p8.png` or `level2.p8`, and a name with its
 * extension is that file. A cart picks the name, so it never reaches outside [folder].
 */
class FolderCarts(
    private val folder: File,
) : KikoPlayerHost {
    override fun loadCart(name: String): ByteArray? {
        val base = name.removePrefix("#")
        if ('/' in base || '\\' in base) return null
        val exact = listOf(base, "$base.p8.png", "$base.p8").map { File(folder, it) }.firstOrNull { it.isFile }
        return (exact ?: if (name.startsWith("#")) latestVersionOf(base) else null)?.readBytes()
    }

    private fun latestVersionOf(id: String): File? =
        folder
            .listFiles { file -> file.isFile }
            .orEmpty()
            .mapNotNull { file ->
                file.name
                    .removeSurrounding("$id-", ".p8.png")
                    .toIntOrNull()
                    ?.let { it to file }
            }.maxByOrNull { (version, _) -> version }
            ?.second
}
