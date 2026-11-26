package com.pixubi.game

import android.content.Context
import kotlin.random.Random

internal enum class GenesisTrait(val title:String){
    CURIOUS("Допитливий"), CALM("Спокійний"), BRAVE("Хоробрий"), PLAYFUL("Грайливий"), PRECISE("Уважний")
}
internal enum class GenesisCare(val title:String,val amount:Int,val bit:Int){
    CHARGE("Зарядити",15,1), SYNC("Синхронізувати",10,2), TOUCH("Стабілізувати ядро",10,4)
}
internal data class GenesisState(val series:RobotSeries,val skin:RobotSkin,val trait:GenesisTrait,val mystery:Boolean,
    val energy:Int=0,val careMask:Int=0,val born:Boolean=false,val name:String="",val rare:Boolean=false){
    val ready get()=!born&&energy>=100&&careMask==7
    fun care(kind:GenesisCare):GenesisState=if(born) this else copy(energy=(energy+kind.amount).coerceAtMost(100),careMask=careMask or kind.bit)
    fun hatch()=if(ready)copy(born=true)else this
    fun rename(value:String)=if(born)copy(name=value.trim().take(24))else this
}
internal object GenesisRules {
    fun create(series:RobotSeries?,random:Random=Random.Default):GenesisState{
        val mystery=series==null
        val chosen=series?:RobotSeries.entries.random(random)
        val rare=mystery&&random.nextInt(100)<8
        val normal=RobotSkin.entries.filter{it!=RobotSkin.GOLD}
        val skin=if(rare)RobotSkin.GOLD else normal.random(random)
        return GenesisState(chosen,skin,GenesisTrait.entries.random(random),mystery,rare=rare)
    }
    fun vessel(s:RobotSeries)=when(s){
        RobotSeries.CLASSIC->"PIXU CUBE · куб"
        RobotSeries.COMPANION->"PIXU ORB · сфера"
        RobotSeries.KAGE->"PIXU POD · капсула"
        RobotSeries.GLITCH->"PIXU SEED · кристал"
        RobotSeries.VALKYRIE->"PIXU BLOOM · механічна квітка"
        RobotSeries.NEXUS->"PIXU QUANTUM · квантове ядро"
        RobotSeries.SYMBIONT->"PIXU SPORE · біомеханічна спора"
        RobotSeries.MYCELIA->"PIXU COCOON · живий енергетичний кокон"
    }
}
internal object GenesisStorage{
    private fun prefs(c:Context,world:Boolean)=c.getSharedPreferences(if(world)"pixubi_world_genesis_v1" else "robot_genesis_v1",Context.MODE_PRIVATE)
    fun load(c:Context,world:Boolean=false):GenesisState?{
        val p=prefs(c,world)
        val series=RobotSeries.entries.firstOrNull{it.name==p.getString("series",null)}?:return null
        val skin=RobotSkin.entries.firstOrNull{it.name==p.getString("skin",null)}?:RobotSkin.BLUE
        val trait=GenesisTrait.entries.firstOrNull{it.name==p.getString("trait",null)}?:GenesisTrait.CALM
        return GenesisState(series,skin,trait,p.getBoolean("mystery",false),p.getInt("energy",0).coerceIn(0,100),p.getInt("care",0) and 7,p.getBoolean("born",false),p.getString("name","").orEmpty(),p.getBoolean("rare",false))
    }
    fun save(c:Context,s:GenesisState?,world:Boolean=false){
        val p=prefs(c,world).edit()
        if(s==null){p.clear().apply();return}
        p.putString("series",s.series.name).putString("skin",s.skin.name).putString("trait",s.trait.name)
            .putBoolean("mystery",s.mystery).putInt("energy",s.energy).putInt("care",s.careMask)
            .putBoolean("born",s.born).putString("name",s.name).putBoolean("rare",s.rare).apply()
    }
}
