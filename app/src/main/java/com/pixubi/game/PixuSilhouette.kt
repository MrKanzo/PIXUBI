package com.pixubi.game

/** Small structural variants of the original artwork. Species-specific crest,
 * outer ear or feather tips are made from adjacent native colors. No stickers,
 * separate eyes or gender accessories floating over the body. */
internal object PixuSilhouette {
    fun female(src:IntArray,w:Int,h:Int,s:Species):IntArray {
        require(w>0&&h>0&&src.size==w*h)
        val out=src.copyOf()
        fun visible(c:Int)=(c ushr 24)>=128
        val opaque=src.indices.filter{visible(src[it])}
        if(opaque.isEmpty())return out
        val minX=opaque.minOf{it%w};val maxX=opaque.maxOf{it%w}
        val minY=opaque.minOf{it/w};val maxY=opaque.maxOf{it/w}
        val span=(maxX-minX+1).coerceAtLeast(1)
        val height=(maxY-minY+1).coerceAtLeast(1)
        val unit=(w/48).coerceAtLeast(1)
        // Use the upper silhouette only. The face and its eye coordinates
        // remain unchanged. Pick a native crest/ear edge close to the center.
        val target=when(s){
            Species.LISYK -> .37f
            Species.ISKRYK -> .44f
            Species.KRAPLYK -> .64f
            Species.PISHCHANYK -> .32f
            Species.KHMARYK -> .58f
        }
        val xCenter=minX+(span*target).toInt()
        val search=(span*.19f).toInt().coerceAtLeast(unit*2)
        val topLimit=minY+(height*.35f).toInt()
        val edges=((xCenter-search).coerceAtLeast(minX)..(xCenter+search).coerceAtMost(maxX))
            .mapNotNull{x->(minY..topLimit).firstOrNull{visible(src[it*w+x])}?.let{x to it}}
        val pivot=edges.minByOrNull{(x,y)-> y*2+kotlin.math.abs(x-xCenter)}?:return out
        val reach=when(s){Species.PISHCHANYK->2;Species.KRAPLYK->3;Species.KHMARYK->3;else->2}*unit
        for(x in (pivot.first-reach).coerceAtLeast(0)..(pivot.first+reach).coerceAtMost(w-1)){
            val y=edges.firstOrNull{it.first==x}?.second?:continue
            val dist=kotlin.math.abs(x-pivot.first)
            val extension=((reach-dist)*unit/reach+unit/2).coerceAtLeast(0)
            for(d in 1..extension){
                val yy=y-d
                if(yy>=0&&!visible(src[yy*w+x]))out[yy*w+x]=src[y*w+x]
            }
        }
        return out
    }
}
