package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotBalanceTest {
    @Test fun allGearHasStatsAndIsCompatibleWithMk1() {
        RobotGear.entries.forEach {
            assertNotEquals(RobotStats(),RobotBalance.spec(it).delta)
            assertTrue(RobotBalance.compatible(it,RobotTier.MK1))
        }
    }
    @Test fun armorTradesSpeedForProtectionAndRemovalRestoresBase() {
        val empty=RobotEquipment()
        val b=RobotBalance.total(RobotTier.MK1,empty)
        val armor=empty.equip(RobotGear.ARMOR)
        val a=RobotBalance.total(RobotTier.MK1,armor)
        assertEquals(b.defense+15,a.defense)
        assertEquals(b.speed-10,a.speed)
        assertEquals(b,RobotBalance.total(RobotTier.MK1,armor.remove(RobotSlot.CHEST)))
    }
    @Test fun twoHandsStackAndCosmeticsAreIndependent() {
        val e=RobotEquipment().equip(RobotGear.BLASTER_L).equip(RobotGear.BLASTER_R)
        assertEquals(38,RobotBalance.total(RobotTier.MK1,e).attack)
        assertEquals(84,RobotBalance.total(RobotTier.MK1,e).energy)
    }
    @Test fun xpBoundariesAndTierThresholds() {
        assertEquals(1,RobotProgress(99).level)
        assertEquals(2,RobotProgress(100).level)
        assertEquals(RobotTier.MK2,RobotProgress(RobotProgress.requiredXp(5)).nextTier)
        assertEquals(RobotTier.MK3,RobotProgress(RobotProgress.requiredXp(12)).nextTier)
        assertEquals(RobotTier.MK4,RobotProgress(RobotProgress.requiredXp(20)).nextTier)
        assertEquals(30,RobotProgress().gain(Int.MAX_VALUE).level)
        assertEquals(0,RobotProgress().gain(-50).safeXp)
    }
    @Test fun statsNeverBecomeInvalidAndLevelsIncreaseStats() {
        assertEquals(1,RobotStats(speed=-10).bounded().speed)
        assertTrue(RobotBalance.total(RobotTier.MK1,RobotEquipment(),2).durability>100)
    }
}