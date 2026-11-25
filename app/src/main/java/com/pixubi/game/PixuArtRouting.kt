package com.pixubi.game

internal enum class PixuArtSource { POSE_PACK, LISYK_MATURE_PACK, YOUNG_ATLAS, OLDER_ATLAS, ORIGINAL_FEMALE, ORIGINAL_YOUNG_MALE }
internal object PixuArtRouting {
    fun source(s:Species,g:GrowthStage,sex:Sex=Sex.BOY):PixuArtSource=if(sex==Sex.GIRL)PixuArtSource.ORIGINAL_FEMALE else when(GrowthStage.migrate(g)){
        GrowthStage.BABY -> PixuArtSource.POSE_PACK
        GrowthStage.YOUNG -> if(s==Species.LISYK) PixuArtSource.POSE_PACK else PixuArtSource.ORIGINAL_YOUNG_MALE
        GrowthStage.ADULT,GrowthStage.ULTRA -> PixuArtSource.OLDER_ATLAS
        else -> error("Unsupported growth form")
    }
}
internal object PixuRaster {
    // Native 2x2 pixel blocks, without interpolation or added facial features.
    fun pixelate(src:IntArray,n:Int):IntArray {
        require(n>0 && n%2==0 && src.size==n*n)
        val out=IntArray(src.size)
        for(y in 0 until n step 2)for(x in 0 until n step 2){
            val i=y*n+x
            val samples=intArrayOf(src[i],src[i+1],src[i+n],src[i+n+1])
            val visible=samples.filter{it ushr 24 >= 128}
            val c=if(visible.size<2)0 else {
                val r=visible.sumOf{it ushr 16 and 255}/visible.size
                val g=visible.sumOf{it ushr 8 and 255}/visible.size
                val b=visible.sumOf{it and 255}/visible.size
                val best=visible.minByOrNull{
                    val dr=(it ushr 16 and 255)-r
                    val dg=(it ushr 8 and 255)-g
                    val db=(it and 255)-b
                    dr*dr+dg*dg+db*db
                }!!
                best or 0xFF000000.toInt()
            }
            out[i]=c;out[i+1]=c;out[i+n]=c;out[i+n+1]=c
        }
        return out
    }
    fun mirror(src:IntArray,n:Int):IntArray {
        require(n>0&&src.size==n*n)
        return IntArray(src.size){i->src[i/n*n+n-1-i%n]}
    }
}
