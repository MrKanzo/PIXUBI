package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotSkillsTest {
    @Test fun completedActivitiesTrainOnlyTheirSkill() {
        val s=RobotSkills().record(RobotActivity.CHARGE).record(RobotActivity.PUZZLE)
        assertEquals(20,s.xp(RobotSkill.ENERGY))
        assertEquals(30,s.xp(RobotSkill.LOGIC))
        assertEquals(0,s.xp(RobotSkill.COMBAT))
        assertEquals(listOf(RobotSkill.LOGIC),s.specialties)
    }
    @Test fun multipleSkillsCanGrowAndTie() {
        var s=RobotSkills()
        repeat(5){s=s.record(RobotActivity.CHARGE).record(RobotActivity.WALK)}
        assertEquals(1,s.level(RobotSkill.ENERGY))
        assertEquals(1,s.level(RobotSkill.MOBILITY))
        assertEquals(setOf(RobotSkill.ENERGY,RobotSkill.MOBILITY),s.specialties.toSet())
        assertEquals(3,s.bonuses().energy)
        assertEquals(2,s.bonuses().speed)
        assertEquals(103,RobotBalance.total(RobotTier.MK1,RobotEquipment(),1,s.bonuses()).energy)
    }
    @Test fun capAndPersistence() {
        var s=RobotSkills()
        repeat(100){s=s.record(RobotActivity.PUZZLE)}
        assertEquals(1000,s.xp(RobotSkill.LOGIC))
        assertEquals(10,s.level(RobotSkill.LOGIC))
        assertEquals(1f,s.fraction(RobotSkill.LOGIC),0f)
        assertEquals(s,RobotSkills.decode(s.encode()+",UNKNOWN:20,ENERGY:-1"))
    }
    @Test fun zeroSkillsDoNotChangeStats() {
        assertTrue(RobotSkills().specialties.isEmpty())
        assertEquals(RobotStats(),RobotSkills().bonuses())
    }
}