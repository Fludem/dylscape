package org.rsmod.content.skills.construction.house

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HouseCodecTest {
    @Test
    fun `a house survives a round trip`() {
        val house = PlayerOwnedHouse(styleId = 3, locationId = 2, locked = true)
        house.placeStarterGarden(gardenRoomType = 2)
        val parlour = PlacedRoom(roomType = 1, rotation = 2)
        parlour.build(slot = 0, furnitureRow = 5517)
        parlour.build(slot = 3, furnitureRow = 5524)
        house.put(RoomKey(PohTemplate.LEVEL_GROUND, 6, 7), parlour)

        val restored = HouseCodec.decode(encoded(house))

        assertEquals(2, restored.roomCount)
        assertEquals(3, restored.styleId)
        assertEquals(2, restored.locationId)
        assertTrue(restored.locked)

        val restoredParlour = restored.get(PohTemplate.LEVEL_GROUND, 6, 7)
        assertEquals(1, restoredParlour?.roomType)
        assertEquals(2, restoredParlour?.rotation)
        assertEquals(5517, restoredParlour?.built(0))
        assertEquals(5524, restoredParlour?.built(3))
        assertEquals(PlacedRoom.EMPTY_HOTSPOT, restoredParlour?.built(1))
        assertEquals(house.entrance, restored.entrance)
    }

    @Test
    fun `a damaged row is skipped rather than thrown on`() {
        val data = CharacterHouseData(0, 0, false, -1, "1,6,6,2,0;nonsense;1,6,7,not-a-number,0")
        val house = HouseCodec.decode(data)

        assertEquals(1, house.roomCount) { "Only the one well-formed room should survive." }
        assertEquals(2, house.get(PohTemplate.LEVEL_GROUND, 6, 6)?.roomType)
    }

    @Test
    fun `removing the entrance room hands the portal to another room`() {
        val house = PlayerOwnedHouse()
        house.placeStarterGarden(gardenRoomType = 2)
        val entrance = house.entrance
        house.put(RoomKey(PohTemplate.LEVEL_GROUND, 6, 7), PlacedRoom(roomType = 1, rotation = 0))

        house.remove(entrance!!)

        assertEquals(RoomKey(PohTemplate.LEVEL_GROUND, 6, 7), house.entrance)
    }

    @Test
    fun `an emptied house has no entrance left`() {
        val house = PlayerOwnedHouse()
        house.placeStarterGarden(gardenRoomType = 2)
        house.remove(house.entrance!!)

        assertNull(house.entrance)
        assertTrue(house.isEmpty())
    }

    private fun encoded(house: PlayerOwnedHouse) =
        CharacterHouseData(
            style = house.styleId,
            location = house.locationId,
            locked = house.locked,
            entrance = house.entrance?.packed ?: -1,
            rooms = HouseCodec.encodeRooms(house),
        )
}
