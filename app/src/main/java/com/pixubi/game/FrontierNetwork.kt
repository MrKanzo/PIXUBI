package com.pixubi.game
internal object FrontierNetwork{
    fun road(q:GridPoint)=q.y in listOf(10,13)||q.x in listOf(15,19)
    fun raids(stage:Int)=listOf(GridPoint(18,11))
    fun gates(stage:Int)=setOf(GridPoint(17,12))
    fun walls(stage:Int):Set<GridPoint> = setOf(GridPoint(16,12),GridPoint(18,12),GridPoint(17,11))+
        when((stage-1)%4){
            0->setOf(GridPoint(17,3),GridPoint(17,4),GridPoint(17,5),GridPoint(8,11))
            1->setOf(GridPoint(16,2),GridPoint(16,3),GridPoint(18,6),GridPoint(11,11))
            2->setOf(GridPoint(17,1),GridPoint(17,2),GridPoint(18,5),GridPoint(6,11))
            else->setOf(GridPoint(16,5),GridPoint(17,5),GridPoint(18,5),GridPoint(12,11))
        }
    fun resources(stage:Int)=mapOf(GridPoint(16,1) to AdventureTile.COINS,GridPoint(19,7) to AdventureTile.ENERGY,GridPoint(9,12) to AdventureTile.REPAIR,GridPoint(17,13) to AdventureTile.AMMO)
    fun portals(stage:Int):Map<GridPoint,GridPoint>{
        val a=GridPoint(16,0);val b=GridPoint(19,10)
        val c=GridPoint(2,10);val d=GridPoint(19,2+(stage-1)%4)
        return mapOf(a to b,b to a,c to d,d to c)
    }
    fun destination(stage:Int,q:GridPoint)=portals(stage)[q]
    fun portalType(q:GridPoint)=if(q==GridPoint(16,0)||q==GridPoint(19,10))0 else 1
    fun locked(a:AdventureState,q:GridPoint)=q in gates(a.stage)&&raids(a.stage).any{AdventureRules.id(it) !in a.cleared}
}
