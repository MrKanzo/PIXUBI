package com.pixubi.game

internal data class WorldSeriesInfo(val role:String,val description:String,val origin:String,val soundtrack:Region)
internal object WorldCatalog{
    fun info(series:RobotSeries):WorldSeriesInfo=when(series){
        RobotSeries.CLASSIC->WorldSeriesInfo("Універсальний механік","Витривалий базовий робот із посиленим захистом.","Перший серійний корпус, відновлений у механічній майстерні.",Region.MOUNTAINS)
        RobotSeries.COMPANION->WorldSeriesInfo("Синхронізатор","Енергоефективний компаньйон із розвиненим інтелектом.","Навчився розпізнавати сигнали життя у неоновій оранжереї.",Region.LAKES)
        RobotSeries.KAGE->WorldSeriesInfo("Тіньовий розвідник","Швидкість, спритність і точні удари.","Протокол тиші, створений у храмі тіней.",Region.MOUNTAINS)
        RobotSeries.GLITCH->WorldSeriesInfo("Кібернетичний хакер","Швидке обчислення та цифрові маневри.","Самостійно зібрав код серед руїн мертвого міста.",Region.DESERT)
        RobotSeries.VALKYRIE->WorldSeriesInfo("Небесна захисниця","Захисні системи й термостійкий корпус.","Охоронний протокол небесної цитаделі.",Region.SKY)
        RobotSeries.NEXUS->WorldSeriesInfo("Квантовий інтелект","Потужне обчислювальне ядро та енергоменеджмент.","Свідомість, яка виникла всередині квантового ядра.",Region.LAKES)
        RobotSeries.SYMBIONT->WorldSeriesInfo("Біомеханічний хижак","Масивні передні лапи, атака та міцний корпус.","Органічний код злився з механічним каркасом у безодні.",Region.DESERT)
        RobotSeries.MYCELIA->WorldSeriesInfo("Біосинтетичний організм","Енергоефективність та відновлення корпусу.","Мережа спор навчилася створювати власні механічні структури.",Region.FOREST)
    }
    fun audio(series:RobotSeries)=info(series).soundtrack
    fun unlocked(p:WorldPet,tier:RobotTier)=tier.ordinal<=p.tier.ordinal
    fun slots(series:RobotSeries,slot:RobotSlot):String{
        if(series!=RobotSeries.SYMBIONT&&series!=RobotSeries.MYCELIA)return slot.title
        return when(slot){RobotSlot.HEAD->"Голова";RobotSlot.CHEST->"Панцир";RobotSlot.LEFT_HAND->"Лівий передній модуль";RobotSlot.RIGHT_HAND->"Правий передній модуль";RobotSlot.BACK->"Спинний модуль";RobotSlot.FEET->"Лапи"}
    }
}
