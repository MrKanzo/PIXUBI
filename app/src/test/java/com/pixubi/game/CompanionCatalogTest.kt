package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class CompanionCatalogTest {
    @Test fun fiveDistinctStagesGrowInOrder() {
        val stages=RobotTier.entries.map{CompanionCatalog.of(it)}
        assertEquals(5,stages.size)
        assertEquals(5,stages.map{it.title}.distinct().size)
        assertTrue(stages.zipWithNext().all{(a,b)->a.scale<b.scale && a.hairLength<b.hairLength})
        assertEquals(0f,stages.first().hairLength,0f)
    }
    @Test fun seriesSelectionDoesNotChangeEquipmentOrTier() {
        val equipped=RobotEquipment().equip(RobotGear.SHIELD_L)
        val original=RobotLoadout(equipment=equipped,tier=RobotTier.MK3)
        val companion=original.copy(series=RobotSeries.COMPANION)
        assertEquals(RobotSeries.CLASSIC,original.series)
        assertEquals(original.equipment,companion.equipment)
        assertEquals(original.tier,companion.tier)
    }
}