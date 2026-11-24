package com.pixubi.game

/** Read-only review scenarios. A null action is the separate sleep-transition candidate. */
internal object AnimationStandPlan {
    data class Item(val action:DevAction?,val title:String,val duration:Float=5f)
    val items=listOf(
        Item(DevAction.IDLE,"Спокій"),
        Item(DevAction.BLINK,"Моргання"),
        Item(DevAction.WALK,"Ходьба"),
        Item(DevAction.RUN,"Біг"),
        Item(DevAction.HOP,"Стрибок"),
        Item(DevAction.HAPPY,"Радість / Ultra"),
        Item(DevAction.SAD,"Сум"),
        Item(DevAction.SURPRISE,"Здивування"),
        Item(DevAction.TIRED,"Втома"),
        Item(DevAction.EAT,"Їжа"),
        Item(DevAction.DRINK,"Вода"),
        Item(DevAction.CHARGE,"Заряд"),
        Item(DevAction.SLEEP,"Сон"),
        Item(DevAction.HIBERNATE,"Сплячка"),
        Item(null,"Засинання / підйом"),
        Item(DevAction.EXPLORE,"Пошук",12f),
        Item(DevAction.DIG,"Нюхання / копання"),
        Item(DevAction.SWIM,"Плавання"),
        Item(DevAction.FLY,"Політ"),
        Item(DevAction.AUTO,"Автономно",12f)
    )
    val duration=items.sumOf{it.duration.toDouble()}.toFloat()
    data class Position(val index:Int,val seconds:Float)
    fun position(seconds:Float):Position{
        var t=if(seconds.isFinite())((seconds%duration)+duration)%duration else 0f
        for((i,item) in items.withIndex()){
            if(t<item.duration)return Position(i,t)
            t-=item.duration
        }
        return Position(0,0f)
    }
    fun supported(s:Species,item:Item)=when(item.action){
        DevAction.DIG -> s==Species.LISYK||s==Species.PISHCHANYK
        DevAction.SWIM -> s==Species.KRAPLYK
        DevAction.FLY -> s==Species.KHMARYK
        else -> true
    }
}
