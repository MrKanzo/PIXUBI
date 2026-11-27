package com.pixubi.game
import androidx.compose.ui.graphics.Color
internal enum class ArenaAttackStyle(val title:String,val reach:Int,val lunge:Boolean,val tint:Color){
    IMPACT("Механічний удар",1,true,Color(0xFFFFDC99)),
    KATANA("Ривок катаною",3,true,Color(0xFFB9EDFF)),
    CLAW("Удар кігтями",1,true,Color(0xFFB8FF9A)),
    PULSE("Імпульсний постріл",3,false,Color(0xFF8DEBFF)),
    HACK("Цифровий імпульс",3,false,Color(0xFFC6A0FF)),
    LIGHT("Промінь енергії",3,false,Color(0xFFFFD5E9)),
    SPORE("Споровий заряд",3,false,Color(0xFFC7FF9A)),
    ARC("Дугова атака",3,false,Color(0xFFB8A7FF));
    companion object {
        fun of(series:RobotSeries,gear:RobotGear?=null):ArenaAttackStyle{
            if(series==RobotSeries.KAGE)return KATANA
            if(gear!=null&&WorldGearCatalog.category(gear)==WorldCategory.WEAPONS){
                return when(gear){
                    RobotGear.BLADE_L,RobotGear.BLADE_R->IMPACT
                    RobotGear.ARC_L,RobotGear.ARC_R->ARC
                    else->PULSE
                }
            }
            return when(series){
                RobotSeries.CLASSIC->IMPACT;RobotSeries.COMPANION->PULSE
                RobotSeries.KAGE->KATANA;RobotSeries.GLITCH,RobotSeries.NEXUS->HACK
                RobotSeries.VALKYRIE->LIGHT;RobotSeries.SYMBIONT->CLAW;RobotSeries.MYCELIA->SPORE
            }
        }
        fun travel(t:Float):Float=when{t<.42f->(t/.42f).coerceIn(0f,1f);t<.60f->1f;else->((1f-t)/.40f).coerceIn(0f,1f)}
        fun combatEquipment(series:RobotSeries,e:RobotEquipment):RobotEquipment{
            if(series!=RobotSeries.KAGE)return e
            return RobotEquipment(e.items.filterValues{WorldGearCatalog.category(it)!=WorldCategory.WEAPONS||it==RobotGear.BLADE_L||it==RobotGear.BLADE_R})
        }
    }
}
