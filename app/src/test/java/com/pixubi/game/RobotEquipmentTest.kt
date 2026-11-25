package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotEquipmentTest {
    @Test fun equipmentCombinesAndReplacesOnlyItsSlot() {
        val e=RobotEquipment().equip(RobotGear.HELMET).equip(RobotGear.ARMOR).equip(RobotGear.SHIELD_L).equip(RobotGear.TOOL_R)
        val next=e.equip(RobotGear.SCANNER)
        assertEquals(RobotGear.SCANNER,next.equipped(RobotSlot.HEAD))
        assertEquals(RobotGear.ARMOR,next.equipped(RobotSlot.CHEST))
        assertEquals(RobotGear.SHIELD_L,next.equipped(RobotSlot.LEFT_HAND))
        assertEquals(RobotGear.TOOL_R,next.equipped(RobotSlot.RIGHT_HAND))
        assertNull(next.remove(RobotSlot.HEAD).equipped(RobotSlot.HEAD))
        assertEquals(4,next.items.size)
    }
    @Test fun loadoutRoundTripAndUnknownIds() {
        val e=RobotEquipment().equip(RobotGear.BOOTS).equip(RobotGear.BATTERY)
        assertEquals(e,RobotEquipment.decode(e.encode()+",REMOVED_ITEM"))
    }
    @Test fun sidesKeepAnatomicalHandIdentity() {
        assertEquals(RobotSlot.RIGHT_HAND,RobotView.RIGHT.nearHand())
        assertEquals(RobotSlot.LEFT_HAND,RobotView.LEFT.nearHand())
        assertEquals(RobotSlot.RIGHT_HAND,RobotView.LEFT.farHand())
    }
}