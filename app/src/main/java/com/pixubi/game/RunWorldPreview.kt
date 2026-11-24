package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun RunWorldPreview(form:ImportedPixuArt.Form,s:Species,g:GrowthStage,sex:Sex,gene:Int,
    pose:ImportedRunReview.Pose,modifier:Modifier=Modifier){
    val context=LocalContext.current.applicationContext
    val cells=remember(context,form.prefix,s,g,sex,gene){
        val authored=ImportedPixuArt.movement(form,SpriteClip.WALK)
        val path=if(authored)form.atlas else ImportedWalkCandidate.asset(s,g,sex)
        val atlas=context.assets.open(path).use{BitmapFactory.decodeStream(it)}
            ?:error("Invalid run source atlas")
        List(12){i->
            val r=if(authored)form.rects.getValue(ImportedRunReview.authoredPose(i))
                else intArrayOf(i%4*128,i/4*128,128,128)
            val raw=Bitmap.createBitmap(atlas,r[0],r[1],128,128)
            val px=IntArray(128*128)
            raw.getPixels(px,0,128,0,0,128,128)
            val leaned=Bitmap.createBitmap(ImportedRunReview.lean(px,i),128,128,Bitmap.Config.ARGB_8888)
            val colored=PixuPalette.apply(leaned,s,Sex.BOY,gene)
            val cell=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888)
            val size=ImportedPixuArt.size(g)
            val x=(192-size)/2
            val y=174-116*size/128
            Canvas(cell).drawBitmap(colored,null,Rect(x,y,x+size,y+size),Paint().apply{isFilterBitmap=false})
            cell.asImageBitmap()
        }
    }
    Image(bitmap=cells[pose.frame],contentDescription=s.title+" — тест бігу",
        modifier=modifier.graphicsLayer{scaleX=if(pose.left)-1f else 1f},
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
