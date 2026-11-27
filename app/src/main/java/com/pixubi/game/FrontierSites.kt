package com.pixubi.game

import kotlin.random.Random

internal enum class FrontierArtifact(val title:String,val description:String){
    SCANNER("Кристал сканера","На карті енергія витрачається кожні 5 кроків замість 4."),
    CORE("Ядро стародавніх","Командир завдає +2 шкоди звичайними та особливими атаками."),
    CHIP("Золотий чип","Додаткові 10 монет із кредитних контейнерів.")
}

internal object FrontierSites {
    private val cache=java.util.concurrent.ConcurrentHashMap<Int,Map<GridPoint,AdventureTile>>()
    fun sites(stage:Int):Map<GridPoint,AdventureTile> = cache.computeIfAbsent(stage){n->
        val l=AdventureRules.layout(n)
        // Preserve every existing resource, encounter and wall, including old saves.
        val blocked=l.walls+l.encounters
        val reachable=mutableSetOf(AdventureRules.base)
        val queue=ArrayDeque<GridPoint>();queue.add(AdventureRules.base)
        while(queue.isNotEmpty()){
            queue.removeFirst().neighbors().filter{AdventureRules.inMap(it)&&it !in blocked&&reachable.add(it)}.forEach{queue.add(it)}
        }
        val candidates=reachable.filter{(it.x>=8||it.y>=6)&&it !in l.resources&&it.x!=4&&!AdventureRules.mainRoad(it)&&it.neighbors().any{q->q in reachable&&AdventureRules.mainRoad(q)}}.sortedWith(compareBy<GridPoint>{it.y}.thenBy{it.x}).shuffled(Random(n*5719+13))
        val kinds=listOf(AdventureTile.LAB,AdventureTile.CACHE,AdventureTile.VAULT)
        candidates.take(3).zip(kinds).toMap()
    }
    fun isBoss(a:AdventureState,q:GridPoint)=AdventureRules.encounters(a).lastOrNull()==q
    fun isVault(a:AdventureState,q:GridPoint)=sites(a.stage)[q]==AdventureTile.VAULT
    fun artifact(stage:Int,offset:Int=0)=FrontierArtifact.entries[(stage-1+offset)%FrontierArtifact.entries.size]
    fun found(a:AdventureState)=sites(a.stage).keys.count{AdventureRules.id(it) in a.cleared}
    fun hostile(tile:AdventureTile)=tile==AdventureTile.ENEMY||tile==AdventureTile.VAULT
}
