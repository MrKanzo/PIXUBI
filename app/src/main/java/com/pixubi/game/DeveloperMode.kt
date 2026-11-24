package com.pixubi.game

/** Preview-only state. Never passed to PetStorage, GameEngine.act or collection observation. */
internal enum class DevAction(val title:String,val emotion:PetEmotion?,val motion:PetMotion?) {
    AUTO("Автономно",null,null),
    IDLE("Спокій",PetEmotion.IDLE,PetMotion.IDLE),
    WALK("Ходьба",PetEmotion.IDLE,PetMotion.WALK),
    RUN("Біг (тест)",PetEmotion.IDLE,PetMotion.WALK),
    HOP("Стрибок",PetEmotion.IDLE,PetMotion.HOP),
    HAPPY("Радість",PetEmotion.HAPPY,PetMotion.BOUNCE),
    SAD("Сум",PetEmotion.SAD,PetMotion.SLOUCH),
    SURPRISE("Здивування",PetEmotion.SURPRISED,PetMotion.HOP),
    TIRED("Втома",PetEmotion.TIRED,PetMotion.TIRED),
    EAT("Їжа",PetEmotion.EATING,PetMotion.CHEW),
    DRINK("Пиття",PetEmotion.DRINKING,PetMotion.DRINK),
    CHARGE("Заряджання",PetEmotion.IDLE,PetMotion.IDLE),
    SLEEP("Сон",PetEmotion.SLEEPING,PetMotion.SLEEP),
    HIBERNATE("Сплячка",PetEmotion.HIBERNATING,PetMotion.SLEEP),
    EXPLORE("Експедиція",PetEmotion.IDLE,null),
    DIG("Копання / нюхання",PetEmotion.IDLE,PetMotion.DIG),
    SWIM("Плавання",PetEmotion.IDLE,PetMotion.SWIM),
    FLY("Політ",PetEmotion.IDLE,PetMotion.FLOAT),
    BLINK("Заплющити очі",PetEmotion.IDLE,PetMotion.IDLE)
}

internal data class DevSpec(
    val species:Species=Species.LISYK,
    val sex:Sex=Sex.GIRL,
    val stage:GrowthStage=GrowthStage.BABY,
    val color:Int=0,
    val region:Region=species.native,
    val hunger:Int=85,
    val thirst:Int=85,
    val energy:Int=85,
    val happiness:Int=80,
    val home:Boolean=false,
    val bowl:Boolean=false,
    val cup:Boolean=false,
    val charger:Boolean=false
) {
    /** Synthetic identity, never copied from the real pet's identity or saved. */
    fun pet(action:DevAction):Pet=Pet(
        species=species,sex=sex,region=region,name="Тестовий Пікс",
        geneA=color.coerceIn(0,5),geneB=0,bornAt=0L,lastUpdatedAt=0L,
        mode=when(action){
            DevAction.SLEEP->Mode.SLEEP
            DevAction.HIBERNATE->Mode.HIBERNATION
            else->Mode.AWAKE
        },
        hunger=hunger.coerceIn(0,100).toDouble(),thirst=thirst.coerceIn(0,100).toDouble(),
        energy=energy.coerceIn(0,100).toDouble(),happiness=happiness.coerceIn(0,100).toDouble(),
        xp=PetGrowth.xpForLevel(stage.minLevel),activeMinutes=stage.minMinutes,
        hasHome=home,hasBowl=bowl,hasCup=cup,hasCharger=charger,
        expeditionStartedAt=if(action==DevAction.EXPLORE)1L else 0L
    )
    companion object {
        fun from(real:Pet?):DevSpec=if(real==null)DevSpec() else DevSpec(
            species=real.species,sex=real.sex,stage=PetGrowth.stage(real),
            color=real.colorIndex,region=real.region,
            hunger=real.hunger.toInt(),thirst=real.thirst.toInt(),
            energy=real.energy.toInt(),happiness=real.happiness.toInt(),
            home=real.hasHome,bowl=real.hasBowl,cup=real.hasCup,charger=real.hasCharger
        )
    }
}

/** Same world-scene code as normal play, but with a deterministic manual clock. */
internal data class DevScene(val action:DevAction,val seconds:Float,val left:Boolean,val gaitPreview:Boolean=false) {
    private val t=if(seconds.isFinite())seconds.coerceAtLeast(0f)else 0f
    val movementCycle:Float get()=(t/2.5f)%1f
    val scoutCycle:Float get()=(t/12f)%4f
    val blinkCycle:Float get()=(t/3.3f)%1f
    val bob:Float get()=1f-kotlin.math.abs((t/.76f)%2f-1f)
}
