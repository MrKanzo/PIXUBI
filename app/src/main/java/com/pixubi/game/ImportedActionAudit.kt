package com.pixubi.game

/** Report the actual available source, not a completion percentage. */
internal object ImportedActionAudit {
    fun describe(f:ImportedPixuArt.Form,s:Species,g:GrowthStage,sex:Sex,action:DevAction,gait:Boolean):String=when(action){
        DevAction.AUTO -> "Автономний вибір; залежить від потреб та доступних поз"
        DevAction.IDLE -> "Оригінальний спрайт + програмні очі й хвіст/крила"
        DevAction.BLINK -> "Програмне моргання оригінальних очей"
        DevAction.WALK -> if(gait&&ImportedWalkCandidate.available(s,g,sex))"Експериментальні 12 кадрів; тільки тест"
            else if(ImportedPixuArt.movement(f,SpriteClip.WALK))"Дві оригінальні пози ходьби"
            else "Локальні рухи лап без прогулянки"
        DevAction.RUN -> if(ImportedPixuArt.movement(f,SpriteClip.WALK))"Прискорена ходьба; окремого бігу немає" else "Прискорені рухи лап на місці"
        DevAction.HOP -> if(ImportedPixuArt.movement(f,SpriteClip.HOP))"Оригінальний стрибок + траєкторія" else "Немає художньої пози стрибка"
        DevAction.HAPPY,DevAction.SAD,DevAction.SURPRISE,DevAction.TIRED -> {
            val name=when(action){DevAction.HAPPY->"happy";DevAction.SAD->"sad";DevAction.SURPRISE->"surprised";else->"tired"}
            if(name in f.frames)"Оригінальна статична емоція" else "Програмна міміка; потрібна художня перевірка"
        }
        DevAction.EAT,DevAction.DRINK -> "Програмні рухи рота; немає повного циклу тіла"
        DevAction.SLEEP -> if("sleep" in f.frames)"Оригінальна поза лежачи + легке дихання" else "Програмно закриті очі; немає пози лежачи"
        DevAction.HIBERNATE -> if("sleep" in f.frames)"Оригінальна нерухома поза; сплячка" else "Програмно закриті очі; немає пози лежачи"
        DevAction.EXPLORE -> "Видова сцена пошуку; рух залежить від доступної анімації"
        DevAction.DIG -> if(s==Species.PISHCHANYK)"Тестове копання лапами" else "Немає окремої пози нюхання/копання"
        DevAction.SWIM -> if(s==Species.KRAPLYK)"Програмний водяний рух" else "Немає видової анімації плавання"
        DevAction.FLY -> if(s==Species.KHMARYK)"Програмний рух крил" else "Немає видової анімації польоту"
    }
}
