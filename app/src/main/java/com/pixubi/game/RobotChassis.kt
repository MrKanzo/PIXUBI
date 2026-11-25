package com.pixubi.game

import androidx.compose.ui.graphics.Color

// Chassis geometry varies around a shared rig. Attachment and rotation pivots remain stable.
internal data class RobotChassis(
    val title:String,val description:String,val shell:Color,val shade:Color,
    val headRadius:Float,val bodyRadius:Float,val limbWidth:Float,val trim:Float
)
internal object RobotChassisCatalog {
    fun of(tier:RobotTier):RobotChassis=when(tier) {
        RobotTier.MK1->RobotChassis("Початковий","Компактний домашній робот",Color(0xFFE5EDF4),Color(0xFF9AAEC3),18f,14f,0f,0f)
        RobotTier.MK2->RobotChassis("Мандрівник","Легкий корпус, сенсорні панелі та посилені гомілки",Color(0xFFD9EDF0),Color(0xFF7C9EAE),12f,10f,1.5f,1f)
        RobotTier.MK3->RobotChassis("Майстер","Сегментований корпус, механічні щоки та радіатори",Color(0xFFC4D1E0),Color(0xFF647B96),8f,7f,2.5f,2f)
        RobotTier.MK4->RobotChassis("Ультра","Багатошаровий корпус, масивні манжети та енергетичні канали",Color(0xFF526B83),Color(0xFF30485F),5f,5f,4f,3f)
    }
    fun unlocked(tier:RobotTier,level:Int)=level>=tier.requiredLevel
}
