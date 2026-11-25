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
    @Test fun refinedHeadProportionsKeepStarterChibi() {
        val stages=RobotTier.entries.map{KageCatalog.of(it)}
        assertTrue(stages.all{it.headScale in 0.73f..0.85f})
        assertTrue(stages.zipWithNext().all{(a,b)->a.headScale>=b.headScale})
        assertEquals(.73f,KageCatalog.of(RobotTier.MK4).headScale,.001f)
        assertTrue(KageRig.ground(RobotTier.MK4)>KageRig.ground(RobotTier.MK1))
        assertEquals(18f*1.72f,KageRig.knee(100f,133f,0f,RobotTier.MK4).y-133f,.001f)
    }
    @Test fun switchingSeriesPreservesEquipment() {
        val original=RobotLoadout(equipment=RobotEquipment().equip(RobotGear.TOOL_R),tier=RobotTier.MK4)
        val ninja=original.copy(series=RobotSeries.KAGE)
        assertEquals(original.equipment,ninja.equipment)
        assertEquals(original.tier,ninja.tier)
        assertEquals(RobotSeries.CLASSIC,original.series)
    }
}
