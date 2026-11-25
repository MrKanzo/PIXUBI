package com.pixubi.game

import android.content.Context

// Only completed gameplay events should call record(). Animation frames never grant XP.
internal enum class RobotSkill(val title:String,val description:String,val bonusPerLevel:RobotStats) {
    ENERGY("Енергоменеджмент","Зарядка та ефективне використання енергії",RobotStats(energy=3,heat=1)),
    ENGINEERING("Інженерія","Догляд, обслуговування та ремонт",RobotStats(durability=3,defense=1)),
    LOGIC("Логіка","Мініігри та головоломки",RobotStats(intelligence=3)),
    EXPLORATION("Дослідження","Сканування та пошук схованок",RobotStats(intelligence=1,heat=1)),
    COLLECTION("Збирач","Знахідки, монети та матеріали",RobotStats(energy=1,intelligence=1)),
    MOBILITY("Мобільність","Прогулянки та активні ігри",RobotStats(speed=2)),
    COMBAT("Бойова підготовка","Майбутні тренування та арени",RobotStats(attack=2,defense=1))
}
internal enum class RobotActivity(val title:String,val skill:RobotSkill,val points:Int,val robotXp:Int,val animation:RobotAction) {
    CHARGE("Зарядитися",RobotSkill.ENERGY,20,5,RobotAction.CHARGE),
    MAINTAIN("Обслуговування",RobotSkill.ENGINEERING,20,5,RobotAction.IDLE),
    PUZZLE("Мінігра",RobotSkill.LOGIC,30,10,RobotAction.WAVE),
    EXPLORE("Дослідити",RobotSkill.EXPLORATION,25,8,RobotAction.SCAN),
    COLLECT("Знайти ресурс",RobotSkill.COLLECTION,25,8,RobotAction.SCAN),
    WALK("Прогулянка",RobotSkill.MOBILITY,20,5,RobotAction.WALK)
}
internal data class RobotSkills(val points:Map<RobotSkill,Int> = emptyMap()) {
    fun xp(skill:RobotSkill)= (points[skill]?:0).coerceIn(0,MAX_XP)
    fun level(skill:RobotSkill)=(xp(skill)/100).coerceAtMost(MAX_LEVEL)
    fun fraction(skill:RobotSkill)=if(level(skill)==MAX_LEVEL)1f else (xp(skill)%100)/100f
    fun record(activity:RobotActivity):RobotSkills {
        val k=activity.skill
        return copy(points=points+(k to (xp(k)+activity.points).coerceAtMost(MAX_XP)))
    }
    val specialties:List<RobotSkill> get() {
        val top=RobotSkill.entries.maxOf{xp(it)}
        return if(top==0)emptyList() else RobotSkill.entries.filter{xp(it)==top}
    }
    fun bonuses():RobotStats=RobotSkill.entries.fold(RobotStats()){sum,k->
        var result=sum
        repeat(level(k)){result+=k.bonusPerLevel}
        result
    }
    fun encode()=RobotSkill.entries.filter{xp(it)>0}.joinToString(","){"${it.name}:${xp(it)}"}
    companion object {
        const val MAX_LEVEL=10
        const val MAX_XP=1000
        fun decode(value:String):RobotSkills {
            val map=mutableMapOf<RobotSkill,Int>()
            value.split(",").forEach { token ->
                val parts=token.split(":")
                if(parts.size==2) {
                    val skill=RobotSkill.entries.firstOrNull{it.name==parts[0]}
                    val xp=parts[1].toIntOrNull()
                    if(skill!=null && xp!=null && xp>0)map[skill]=xp.coerceAtMost(MAX_XP)
                }
            }
            return RobotSkills(map)
        }
    }
}
internal object RobotSkillsStorage {
    private const val PREFS="robot_lab_skills_v1"
    fun load(context:Context)=RobotSkills.decode(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("skills","").orEmpty())
    fun save(context:Context,skills:RobotSkills) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("skills",skills.encode()).apply()
    }
}
