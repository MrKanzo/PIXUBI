package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotChassisTest {
    @Test fun everyTierHasDistinctChassis() {
        val all=RobotTier.entries.map{RobotChassisCatalog.of(it)}
        assertEquals(5,all.map{it.title}.distinct().size)
        assertEquals(5,all.map{it.headRadius}.distinct().size)
        assertEquals(0f,all.first().limbWidth,0f)
        assertTrue(all.zipWithNext().all{(a,b)->a.limbWidth<b.limbWidth})
    }
    @Test fun unlockThresholds() {
        assertTrue(RobotChassisCatalog.unlocked(RobotTier.MK1,1))
        assertFalse(RobotChassisCatalog.unlocked(RobotTier.MK2,4))
        assertTrue(RobotChassisCatalog.unlocked(RobotTier.MK2,5))
        assertFalse(RobotChassisCatalog.unlocked(RobotTier.MK4,19))
        assertTrue(RobotChassisCatalog.unlocked(RobotTier.MK4,20))
        assertFalse(RobotChassisCatalog.unlocked(RobotTier.OMEGA,30))
        assertEquals(RobotTier.MK4,RobotProgress(RobotProgress.MAX_XP).nextTier)
    }
    @Test fun gearWorksOnAllCurrentChassis() {
        val equipment=RobotEquipment().equip(RobotGear.ARMOR)
        RobotTier.entries.forEach { tier ->
            assertTrue(RobotBalance.compatible(RobotGear.ARMOR,tier))
            assertEquals(RobotBalance.base(tier).defense+15,RobotBalance.total(tier,equipment).defense)
        }
    }
}