package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.*

/** Transparent, hand-cropped art from the approved KAGE concept. The atlas is local/offline. */
private data class ArtPiece(val pivot:Offset,val mask:Path)
private fun piece(x:Float,y:Float,vararg coords:Int):ArtPiece {
    val mask=Path().apply {
        moveTo(coords[0].toFloat(),coords[1].toFloat())
        for(i in 2 until coords.size step 2)lineTo(coords[i].toFloat(),coords[i+1].toFloat())
        close()
    }
    return ArtPiece(Offset(x,y),mask)
}
private data class ArtRig(
    val src: IntOffset,val size:IntSize,val center:Float,
    val arms:List<ArtPiece>,val thighs:List<ArtPiece>,val shins:List<ArtPiece>
) {
    val parts=arms+thighs+shins
}
private val frontArt=ArtRig(IntOffset(0,0),IntSize(85,140),53f,
    listOf(
        piece(41f,40f, 31,36,43,35,46,43,42,50,38,55,39,63,36,71,35,83,24,83,25,67,26,57,31,50),
        piece(66f,40f, 61,36,70,35,73,43,73,51,77,56,81,68,80,83,69,83,66,73,67,64,63,56,59,46)
    ),
    listOf(
        piece(45f,76f, 36,75,50,76,53,81,48,91,44,99,33,98,33,91,37,83),
        piece(60f,76f, 54,76,68,75,69,85,70,94,65,99,56,98,54,89,52,81)
    ),
    listOf(
        piece(40f,96f, 33,94,45,94,45,103,42,116,39,126,40,132,39,140,23,140,23,135,28,129,30,121,31,110,30,102),
        piece(61f,96f, 55,94,68,94,70,103,70,116,73,126,79,131,84,136,84,140,59,140,59,132,60,123,57,111,55,103)
    )
)
// The source profile faces right; left is the mirror of the complete rig.
private val sideArt=ArtRig(IntOffset(85,3),IntSize(52,137),34f,
    listOf(piece(32f,37f, 24,33,37,33,40,43,39,52,43,58,42,68,40,77,30,82,25,75,26,65,27,53,25,45)),
    listOf(piece(33f,75f, 23,73,39,74,42,81,39,90,36,97,24,96,23,89,24,82)),
    listOf(piece(30f,94f, 24,91,38,93,40,102,36,116,36,125,40,129,46,133,46,137,19,137,19,130,22,124,23,115,21,105,22,98))
)

private fun DrawScope.artWhole(image:ImageBitmap,rig:ArtRig,alpha:Float=1f) {
    drawImage(image,srcOffset=rig.src,srcSize=rig.size,dstOffset=IntOffset.Zero,dstSize=rig.size,alpha=alpha)
}
private fun DrawScope.artPart(image:ImageBitmap,rig:ArtRig,part:ArtPiece,angle:Float,shift:Offset=Offset.Zero,alpha:Float=1f) {
    translate(shift.x,shift.y) {
        rotate(angle,part.pivot) { clipPath(part.mask) { artWhole(image,rig,alpha) } }
    }
}
private fun DrawScope.artBody(image:ImageBitmap,rig:ArtRig,index:Int=0) {
    if(index>=rig.parts.size) artWhole(image,rig)
    else clipPath(rig.parts[index].mask,ClipOp.Difference) { artBody(image,rig,index+1) }
}
private fun rotated(v:Offset,degrees:Float):Offset {
    val r=degrees*PI.toFloat()/180f
    return Offset(v.x*cos(r)-v.y*sin(r),v.x*sin(r)+v.y*cos(r))
}
private fun DrawScope.artLeg(image:ImageBitmap,rig:ArtRig,i:Int,thigh:Float,shin:Float,shift:Offset=Offset.Zero,alpha:Float=1f) {
    val upper=rig.thighs[i];val lower=rig.shins[i]
    val delta=lower.pivot-upper.pivot
    val kneeShift=rotated(delta,thigh)-delta+shift
    artPart(image,rig,upper,thigh,shift,alpha)
    artPart(image,rig,lower,shin,kneeShift,alpha)
}

@Composable
internal fun KageReferencePreview(view:RobotView,action:RobotAction,face:RobotFace,phase:Float,modifier:Modifier=Modifier) {
    val image=ImageBitmap.imageResource(R.drawable.kage_reference)
    val side=view!=RobotView.FRONT
    val rig=if(side)sideArt else frontArt
    val dash=if(action==RobotAction.NINJA_DASH)KageDashMotion.pose(phase) else null
    val pose=RobotMotion.pose(action,phase)
    val moving=action in listOf(RobotAction.WALK,RobotAction.RUN,RobotAction.JUMP,RobotAction.WAVE,RobotAction.ATTACK,RobotAction.NINJA_DASH)
    Canvas(modifier) {
        val fit=min(size.width/(if(dash!=null)205f else 115f),size.height/175f)
        val dir=if(view==RobotView.LEFT)-1f else 1f
        val travel=if(dash!=null&&side)(dash.travel-44f)*.68f*dir else 0f
        val ground=(size.height+rig.size.height*fit)/2f+4f*fit
        drawOval(Color(0xFF142338).copy(alpha=.28f),Offset(size.width/2f+travel*fit-27f*fit,ground-1f*fit),Size(54f*fit,5f*fit))
        translate(size.width/2f+travel*fit,ground) {
            val depth=if(dash!=null&&!side)1f+dash.travel/550f else 1f
            scale(fit*dir*depth,fit*depth,Offset.Zero) {
                val top=-rig.size.height.toFloat()+pose.lift*.6f+(dash?.drop?:0f)*.6f
                translate(-rig.center,top) {
                    if(dash!=null&&dash.trail>0f&&side)for(i in 0..3) {
                        val y=47f+i*15f
                        drawLine(Color(0xFF26DFF0).copy(alpha=dash.trail*(.5f-i*.09f)),Offset(-35f-i*5f,y),Offset(-7f-i*5f,y),1.5f,StrokeCap.Round)
                    }
                    if(!moving)artWhole(image,rig)
                    else {
                        val near=dash?.nearThigh?:pose.nearLeg
                        val far=dash?.farThigh?:pose.farLeg
                        val ns=dash?.nearShin?:near
                        val fs=dash?.farShin?:far
                        val na=dash?.nearArm?:pose.nearArm
                        val fa=dash?.farArm?:pose.farArm
                        if(side) {
                            // The original profile has one visible limb; reuse it as the occluded limb only in motion.
                            artLeg(image,rig,0,far,fs,Offset(-3f,-1f),.7f)
                            artPart(image,rig,rig.arms[0],fa,Offset(-3f,-1f),.7f)
                            artLeg(image,rig,0,near,ns)
                            artBody(image,rig)
                            artPart(image,rig,rig.arms[0],na)
                        } else {
                            artLeg(image,rig,0,far*.18f,fs*.18f)
                            artLeg(image,rig,1,near*.18f,ns*.18f)
                            artBody(image,rig)
                            artPart(image,rig,rig.arms[0],RobotMotion.frontArmAngle(fa,false)-8f)
                            artPart(image,rig,rig.arms[1],RobotMotion.frontArmAngle(na,true)+8f)
                        }
                    }
                    // The original eyes are preserved for the neutral face. Override only the visor zone.
                    if(face!=RobotFace.NEUTRAL||action==RobotAction.SLEEP||phase>.95f&&action==RobotAction.IDLE) {
                        val cx=if(side)40f else 53f
                        val cy=if(side)22f else 24f
                        val xs=if(side)listOf(cx)else listOf(cx-5f,cx+5f)
                        val sleepy=action==RobotAction.SLEEP||face==RobotFace.SLEEP||phase>.95f&&action==RobotAction.IDLE
                        for(x in xs) {
                            drawOval(Color(0xFF222632),Offset(x-4f,cy-2.8f),Size(8f,5.5f))
                            val light=Color(0xFF26DFF0)
                            if(sleepy)drawLine(light,Offset(x-2.5f,cy),Offset(x+2.5f,cy),.9f,StrokeCap.Round)
                            else if(face==RobotFace.HAPPY) {
                                val eye=Path().apply{moveTo(x-2.8f,cy+1f);quadraticTo(x,cy-4f,x+2.8f,cy+1f)}
                                drawPath(eye,light,style=Stroke(1f,cap=StrokeCap.Round))
                            } else if(face==RobotFace.SURPRISE)drawCircle(light,1.8f,Offset(x,cy))
                            else drawLine(light,Offset(x-2.8f,cy-1f),Offset(x+2.8f,cy+1f),1.5f,StrokeCap.Round)
                        }
                    }
                    if(action==RobotAction.CHARGE)drawCircle(Color(0xFF26DFF0).copy(alpha=(1f-phase)*.6f),8f+phase*9f,Offset(rig.center,43f),style=Stroke(1f))
                }
            }
        }
    }
}
