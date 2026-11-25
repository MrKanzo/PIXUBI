package com.pixubi.game

import android.content.Context
import androidx.compose.ui.graphics.Color

internal enum class RobotGear(val title:String,val slot:RobotSlot) {
    SCANNER("Сканер",RobotSlot.HEAD),
    HELMET("Шолом",RobotSlot.HEAD),
    ARMOR("Броня",RobotSlot.CHEST),
    REACTOR("Реактор",RobotSlot.CHEST),
    SHIELD_L("Щит",RobotSlot.LEFT_HAND),
    TOOL_L("Інструмент",RobotSlot.LEFT_HAND),
    BLASTER_L("Бластер",RobotSlot.LEFT_HAND),
    SHIELD_R("Щит",RobotSlot.RIGHT_HAND),
    TOOL_R("Інструмент",RobotSlot.RIGHT_HAND),
    BLASTER_R("Бластер",RobotSlot.RIGHT_HAND),
    BATTERY("Батарея",RobotSlot.BACK),
    BOOSTER("Прискорювач",RobotSlot.BACK),
    BOOTS("Захисні черевики",RobotSlot.FEET),
    SPEED_BOOTS("Швидкісні черевики",RobotSlot.FEET);
    companion object {
        fun forSlot(slot:RobotSlot)=entries.filter { it.slot==slot }
    }
}
internal data class RobotEquipment(val items:Map<RobotSlot,RobotGear> = emptyMap()) {
    fun equipped(slot:RobotSlot):RobotGear?=items[slot]?.takeIf{it.slot==slot}
    fun equip(gear:RobotGear)=copy(items=items+(gear.slot to gear))
    fun remove(slot:RobotSlot)=copy(items=items-slot)
    fun encode():String=RobotSlot.entries.mapNotNull { equipped(it)?.name }.joinToString(",")
    companion object {
        fun decode(value:String):RobotEquipment = value.split(",").fold(RobotEquipment()) { current,id ->
            RobotGear.entries.firstOrNull{it.name==id}?.let{current.equip(it)}?:current
        }
    }
}
internal enum class RobotSkin(val title:String,val accent:Color) {
    BLUE("Синій",Color(0xFF268ED2)), GREEN("Зелений",Color(0xFF29AB83)),
    RED("Червоний",Color(0xFFC94A5C)), GOLD("Золотий",Color(0xFFE0A634)),
    PURPLE("Фіолетовий",Color(0xFF9B74D8)), GRAPHITE("Графіт",Color(0xFF708398)), PINK("Рожевий",Color(0xFFDF64AB)), CYAN("Бірюзовий",Color(0xFF26DFF0))
}
internal object RobotLabStorage {
    fun loadSymbiontSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("symbiont_skin","GREEN")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.GREEN
    }
    fun saveSymbiontSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("symbiont_skin",skin.name).apply()
    }
    fun loadNexusSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("nexus_skin","CYAN")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.CYAN
    }
    fun saveNexusSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("nexus_skin",skin.name).apply()
    }
    fun loadValkyrieSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("valkyrie_skin","PINK")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.PINK
    }
    fun saveValkyrieSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("valkyrie_skin",skin.name).apply()
    }
    fun loadGlitchSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("glitch_skin","PURPLE")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.PURPLE
    }
    fun saveGlitchSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("glitch_skin",skin.name).apply()
    }
    fun loadKageSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("kage_skin","CYAN")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.CYAN
    }
    fun saveKageSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("kage_skin",skin.name).apply()
    }
    fun loadCompanionSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("companion_skin","PINK")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.PINK
    }
    fun saveCompanionSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("companion_skin",skin.name).apply()
    }
    fun load(context:Context):Pair<RobotEquipment,RobotSkin> {
        val prefs=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE)
        return RobotEquipment.decode(prefs.getString("gear","").orEmpty()) to
            (RobotSkin.entries.firstOrNull{it.name==prefs.getString("skin","BLUE")}?:RobotSkin.BLUE)
    }
    fun save(context:Context,equipment:RobotEquipment,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit()
            .putString("gear",equipment.encode()).putString("skin",skin.name).apply()
    }
}
internal fun RobotView.nearHand():RobotSlot = if(this==RobotView.LEFT) RobotSlot.LEFT_HAND else RobotSlot.RIGHT_HAND
internal fun RobotView.farHand():RobotSlot = if(nearHand()==RobotSlot.RIGHT_HAND) RobotSlot.LEFT_HAND else RobotSlot.RIGHT_HAND
