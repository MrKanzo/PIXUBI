package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class NativeLimbRigTest {
    @Test fun allSpeciesAndStagesHavePawsAndAppendages() {
        for(s in Species.entries) for(stage in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT)) {
            val joints=NativeLimbRig.joints(s,stage)
            assertTrue(joints.count { it.kind==LimbKind.PAW }>=2)
            assertTrue(joints.any { it.kind!=LimbKind.PAW })
            if(s==Species.KHMARYK) assertTrue(joints.count { it.kind==LimbKind.WING }==2)
        }
    }
    @Test fun walkingAlternatesPawsAndTailWags() {
        for(s in Species.entries) for(stage in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT)) {
            val joints=NativeLimbRig.joints(s,stage)
            for(p in joints.filter { it.kind==LimbKind.PAW }) {
                val a=NativeLimbRig.offset(listOf(p),p.x,p.y,PetMotion.WALK,1f/12f)
                val b=NativeLimbRig.offset(listOf(p),p.x,p.y,PetMotion.WALK,3f/12f)
                assertTrue(a.dx*b.dx<0f)
                assertTrue(a.dy<0f || b.dy<0f)
            }
            val tail=joints.first { it.kind!=LimbKind.PAW }
            val a=NativeLimbRig.offset(listOf(tail),tail.x,tail.y,PetMotion.IDLE,.125f)
            val b=NativeLimbRig.offset(listOf(tail),tail.x,tail.y,PetMotion.IDLE,.375f)
            assertTrue(a!=b)
        }
    }
    @Test fun sleepIsStillAndFaceIsNotAnimatedByPaws() {
        for(s in Species.entries) for(stage in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT)) {
            val joints=NativeLimbRig.joints(s,stage)
            val n=if(stage==GrowthStage.ADULT)160 else 192
            val a=NativeLimbRig.vertices(n,n,joints,PetMotion.SLEEP,.1f)
            val b=NativeLimbRig.vertices(n,n,joints,PetMotion.SLEEP,.7f)
            assertArrayEquals(a,b,0f)
            val c=NativeLimbRig.vertices(n,n,joints,PetMotion.WALK,.125f)
            assertFalse(c.contentEquals(a))
            assertEquals(LimbOffset(0f,0f),NativeLimbRig.offset(joints,n/2f,15f,PetMotion.WALK,.125f))
            assertEquals(0f,c[0],0f)
            assertEquals(0f,c[1],0f)
            assertTrue(c.all { it.isFinite() })
        }
    }
}
