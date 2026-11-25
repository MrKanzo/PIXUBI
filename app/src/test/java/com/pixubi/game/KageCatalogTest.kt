package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class KageCatalogTest {
    @Test fun fiveStagesProgressivelyGrow() {
        val stages=RobotTier.entries.map{KageCatalog.of(it)}
        assertEquals(5,stages.size)
        assertEquals(5,stages.map{it.title}.distinct().size)
        assertTrue(stages.zipWithNext().all{(a,b)->a.scale<b.scale && a.scarfLength<b.scarfLength && a.armor<b.armor})
    }
    @Test fun switchingSeriesPreservesEquipment() {
        val original=RobotLoadout(equipment=RobotEquipment().equip(RobotGear.TOOL_R),tier=RobotTier.MK4)
        val ninja=original.copy(series=RobotSeries.KAGE)
        assertEquals(original.equipment,ninja.equipment)
        assertEquals(original.tier,ninja.tier)
        assertEquals(RobotSeries.CLASSIC,original.series)
    }
}
