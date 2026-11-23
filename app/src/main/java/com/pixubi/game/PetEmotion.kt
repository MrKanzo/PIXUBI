package com.pixubi.game

/** Visual emotions are short-lived, independent from save-data so old pets still load. */
enum class PetEmotion {
    IDLE, HAPPY, SAD, SLEEPING, EATING, DRINKING, TIRED, SURPRISED, HIBERNATING
}

object EmotionResolver {
    fun resolve(pet: Pet, temporary: PetEmotion? = null): PetEmotion = when {
        pet.mode == Mode.HIBERNATION -> PetEmotion.HIBERNATING
        pet.mode == Mode.SLEEP -> PetEmotion.SLEEPING
        temporary != null -> temporary
        pet.hunger < 25 || pet.thirst < 25 || pet.happiness < 27 -> PetEmotion.SAD
        pet.energy < 25 -> PetEmotion.TIRED
        pet.happiness > 90 -> PetEmotion.HAPPY
        else -> PetEmotion.IDLE
    }

    fun label(emotion: PetEmotion): String = when (emotion) {
        PetEmotion.IDLE -> "Спокійний"
        PetEmotion.HAPPY -> "Радіє!"
        PetEmotion.SAD -> "Сумує"
        PetEmotion.SLEEPING -> "Солодко спить"
        PetEmotion.EATING -> "Смачно їсть"
        PetEmotion.DRINKING -> "П'є водичку"
        PetEmotion.TIRED -> "Втомився"
        PetEmotion.SURPRISED -> "Ой! Здивувався"
        PetEmotion.HIBERNATING -> "У сплячці"
    }

    fun bubble(pet: Pet, emotion: PetEmotion): String? = when (emotion) {
        PetEmotion.EATING -> "🍓 Ням-ням!"
        PetEmotion.DRINKING -> "💧 Буль-буль!"
        PetEmotion.SLEEPING -> "💤 Zzz…"
        PetEmotion.HIBERNATING -> "❄️ Пауза"
        PetEmotion.HAPPY -> "💖 Ура!"
        PetEmotion.SURPRISED -> "✨ Ого!"
        PetEmotion.TIRED -> "🥱 Сон…"
        PetEmotion.SAD -> when {
            pet.thirst < 25 -> "💧 Хочу пити"
            pet.hunger < 25 -> "🍓 Я голодний"
            else -> "🥺 Пограймося?"
        }
        PetEmotion.IDLE -> null
    }

    fun caption(pet: Pet, emotion: PetEmotion): String = when (emotion) {
        PetEmotion.HIBERNATING -> "Я сплю у сплячці. Час і старіння на паузі."
        PetEmotion.SLEEPING -> "Ш-ш-ш… Відновлюю сили."
        PetEmotion.EATING -> "Ням-ням! Смачненько! 🍓"
        PetEmotion.DRINKING -> "Ох, як освіжає! 💧"
        PetEmotion.HAPPY -> "Я такий щасливий, що аж підстрибую! ✨"
        PetEmotion.SAD -> "Мені трохи сумно. Подбай про мене, будь ласка."
        PetEmotion.TIRED -> "Очі злипаються… Хочу відпочити."
        PetEmotion.SURPRISED -> "Ой! Ти мене полоскотав! 😮"
        PetEmotion.IDLE -> pet.message
    }
}
