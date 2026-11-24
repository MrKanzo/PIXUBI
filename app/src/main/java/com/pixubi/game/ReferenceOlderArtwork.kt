package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** New original artwork for all five species, adult and ultra.
 * 5 columns in Species order; two rows: adult, ultra. Native 192px cells. */
internal object ReferenceOlderArtwork {
    const val ASSET="pixubi_older_v032.png"
    const val CELL=192
    private var atlas:Bitmap?=null

    @Synchronized fun pixels(context:Context,s:Species,g:GrowthStage):IntArray{
        require(g==GrowthStage.ADULT||g==GrowthStage.ULTRA)
        val src=atlas?:context.assets.open(ASSET).use{
            BitmapFactory.decodeStream(it)?:error("PIXUBI older atlas is invalid")
        }.also{
            require(it.width==CELL*5&&it.height==CELL*2)
            atlas=it
        }
        val x=s.ordinal*CELL
        val y=if(g==GrowthStage.ULTRA)CELL else 0
        return IntArray(CELL*CELL).also{src.getPixels(it,0,CELL,x,y,CELL,CELL)}
    }
}
