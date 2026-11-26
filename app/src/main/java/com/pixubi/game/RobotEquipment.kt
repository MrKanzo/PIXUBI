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
    BLADE_L("Плазмовий клинок",RobotSlot.LEFT_HAND),
    BLADE_R("Плазмовий клинок",RobotSlot.RIGHT_HAND),
    CANNON_L("Іонна гармата",RobotSlot.LEFT_HAND),
    CANNON_R("Іонна гармата",RobotSlot.RIGHT_HAND),
    BATTERY("Батарея",RobotSlot.BACK),
    BOOSTER("Прискорювач",RobotSlot.BACK),
    BOOTS("Захисні черевики",RobotSlot.FEET),
    SPEED_BOOTS("Швидкісні черевики",RobotSlot.FEET),
    VISOR("Тактичний візор",RobotSlot.HEAD),
    LIGHT_ARMOR("Легка композитна броня",RobotSlot.CHEST),
    HEAVY_ARMOR("Важка екзоброня",RobotSlot.CHEST),
    POWERBANK_MINI("Павербанк MINI",RobotSlot.BACK),
    POWERBANK_MAX("Павербанк TITAN",RobotSlot.BACK),
    REPAIR_DRONE("Ремонтний дрон",RobotSlot.BACK),
    MAG_BOOTS("Магнітні опори",RobotSlot.FEET),
    RIFLE_L("Імпульсна гвинтівка",RobotSlot.LEFT_HAND),
    RIFLE_R("Імпульсна гвинтівка",RobotSlot.RIGHT_HAND),
    ARC_L("Дуговий випромінювач",RobotSlot.LEFT_HAND),
    ARC_R("Дуговий випромінювач",RobotSlot.RIGHT_HAND),
    REPAIR_L("Мультитул ремонту",RobotSlot.LEFT_HAND),
    REPAIR_R("Мультитул ремонту",RobotSlot.RIGHT_HAND);
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
    fun loadMyceliaSkin(context:Context):RobotSkin {
        val id=context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).getString("mycelia_skin","PURPLE")
        return RobotSkin.entries.firstOrNull{it.name==id}?:RobotSkin.PURPLE
    }
    fun saveMyceliaSkin(context:Context,skin:RobotSkin) {
        context.getSharedPreferences("robot_lab_loadout_v1",Context.MODE_PRIVATE).edit().putString("mycelia_skin",skin.name).apply()
    }
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
