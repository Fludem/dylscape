package org.rsmod.api.cache.types

import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.Js5Configs

/**
 * Throwaway: decodes the identkit config so the character-design screen can be given the real,
 * per-gender style ranges instead of numbers typed off a wiki.
 *
 * Identkit opcode 1 is `bodypart`: 0-6 are the male slots and 7-13 the female ones, in the order
 * hair, jaw, torso, arms, hands, legs, feet. Opcode 2 is the model list; a kit with no models is a
 * "none" entry (the female jaw, mostly) and is still selectable.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PlayerDesignDump {
    @Test
    fun `dump identkit bodyparts`() {
        Cache.open(Path.of(".data/cache/game")).use { cache ->
            val byPart = sortedMapOf<Int, MutableList<Int>>()
            for (file in cache.list(Js5Archives.CONFIG, Js5Configs.IDENTIKIT)) {
                val data = cache.read(Js5Archives.CONFIG, Js5Configs.IDENTIKIT, file.id)
                var bodypart = -1
                try {
                    while (data.isReadable) {
                        when (val code = data.readUnsignedByte().toInt()) {
                            0 -> break
                            1 -> bodypart = data.readUnsignedByte().toInt()
                            2 -> {
                                val count = data.readUnsignedByte().toInt()
                                repeat(count) { data.readUnsignedShort() }
                            }
                            3 -> {}
                            in 40..49 -> {
                                data.readUnsignedShort()
                                data.readUnsignedShort()
                            }
                            in 50..59 -> {
                                data.readUnsignedShort()
                                data.readUnsignedShort()
                            }
                            in 60..69 -> data.readUnsignedShort()
                            else -> break
                        }
                    }
                } finally {
                    data.release()
                }
                if (bodypart >= 0) {
                    byPart.getOrPut(bodypart) { mutableListOf() }.add(file.id)
                }
            }
            val slots = listOf("hair", "jaw", "torso", "arms", "hands", "legs", "feet")
            for ((part, ids) in byPart) {
                val gender = if (part < 7) "male  " else "female"
                val slot = slots.getOrElse(part % 7) { "?" }
                println("  bodypart=$part $gender $slot -> ${ids.sorted()}")
            }
        }
    }
}
