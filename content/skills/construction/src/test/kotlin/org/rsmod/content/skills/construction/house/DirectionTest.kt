package org.rsmod.content.skills.construction.house

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DirectionTest {
    @Test
    fun `a wall tile names the direction it faces`() {
        assertEquals(Direction.South, Direction.ofWall(localX = 3, localZ = 0))
        assertEquals(Direction.North, Direction.ofWall(localX = 3, localZ = 7))
        assertEquals(Direction.West, Direction.ofWall(localX = 0, localZ = 3))
        assertEquals(Direction.East, Direction.ofWall(localX = 7, localZ = 3))
        assertNull(Direction.ofWall(localX = 3, localZ = 3)) { "The middle is not a wall." }
    }

    @Test
    fun `rotating turns a doorway a quarter at a time`() {
        assertEquals(Direction.South, Direction.South.rotate(0))
        assertEquals(Direction.West, Direction.South.rotate(1))
        assertEquals(Direction.North, Direction.South.rotate(2))
        assertEquals(Direction.East, Direction.South.rotate(3))
    }

    @Test
    fun `a room reached from one side has to open onto the other`() {
        val northOnly =
            RoomTemplateInfo(roomType = 1, hotspots = emptyList(), doors = setOf(Direction.North))

        // Walking north into a room means that room needs a door on its south wall, which is two
        // quarter turns away from the one it has.
        assertEquals(2, northOnly.rotationFacing(Direction.South))
        assertEquals(0, northOnly.rotationFacing(Direction.North))

        val doorless = RoomTemplateInfo(roomType = 2, hotspots = emptyList(), doors = emptySet())
        assertEquals(-1, doorless.rotationFacing(Direction.South))
    }

    @Test
    fun `a room key packs and unpacks`() {
        val key = RoomKey(level = 2, gridX = 11, gridZ = 4)
        val restored = RoomKey.unpack(key.packed)

        assertEquals(2, restored.level)
        assertEquals(11, restored.gridX)
        assertEquals(4, restored.gridZ)
        assertEquals(RoomKey(2, 12, 4), key.translate(dx = 1, dz = 0))
    }
}
