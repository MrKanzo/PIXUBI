package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class AnimationStandPlanTest{
    @Test fun includesEveryDeveloperActionExactlyOnceAndSleepTransition(){
        val items=AnimationStandPlan.items
        assertEquals(20,items.size)
        assertEquals(DevAction.entries.toSet(),items.mapNotNull{it.action}.toSet())
        assertEquals(DevAction.entries.size,items.mapNotNull{it.action}.size)
        assertEquals(1,items.count{it.action==null})
        for(s in Species.entries)for(g in GrowthStage.playable)for(sex in Sex.entries){
            val spec=DevSpec(species=s,stage=g,sex=sex,color=3)
            for(item in items){
                val p=spec.pet(item.action?:DevAction.SLEEP)
                assertEquals(s,p.species);assertEquals(sex,p.sex);assertEquals(g,PetGrowth.stage(p))
                assertEquals(3,p.colorIndex)
            }
        }
    }
    @Test fun autoShowVisitsAllItemsAndLoops(){
        var t=0f
        for((i,item) in AnimationStandPlan.items.withIndex()){
            assertEquals(i,AnimationStandPlan.position(t).index)
            val pos=AnimationStandPlan.position(t+item.duration-.01f)
            assertEquals(i,pos.index)
            t+=item.duration
        }
        assertEquals(AnimationStandPlan.duration,t,0f)
        assertEquals(0,AnimationStandPlan.position(t).index)
        assertEquals(0,AnimationStandPlan.position(Float.NaN).index)
    }
}
