package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Draw the facial frame once, deforming only the native limb joints. */
@Composable
internal fun ArticulatedPixuImage(
    bitmap:Bitmap,species:Species,stage:GrowthStage,motion:PetMotion,
    phase:Float,description:String,modifier:Modifier=Modifier
) {
    val joints=remember(species,stage) { NativeLimbRig.joints(species,stage) }
    val paint=remember { Paint().apply { isFilterBitmap=false;isAntiAlias=false } }
    Canvas(modifier.semantics { contentDescription=description }) {
        val scale=minOf(size.width/bitmap.width,size.height/bitmap.height)
        if(scale<=0f) return@Canvas
        val x=(size.width-bitmap.width*scale)/2f
        val y=(size.height-bitmap.height*scale)/2f
        val grid=NativeLimbRig.GRID
        val verts=NativeLimbRig.vertices(bitmap.width,bitmap.height,joints,motion,phase,grid)
        drawIntoCanvas { canvas ->
            val native=canvas.nativeCanvas
            val save=native.save()
            try {
                native.translate(x,y)
                native.scale(scale,scale)
                if(motion==PetMotion.SLEEP || joints.isEmpty())
                    native.drawBitmap(bitmap,0f,0f,paint)
                else native.drawBitmapMesh(bitmap,grid,grid,verts,0,null,0,paint)
            } finally {
                native.restoreToCount(save)
            }
        }
    }
}
