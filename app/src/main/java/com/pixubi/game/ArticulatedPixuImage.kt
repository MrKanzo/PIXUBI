package com.pixubi.game

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/** Legacy image adapter. Mesh animation is intentionally disabled. */
@Composable
internal fun ArticulatedPixuImage(
    bitmap:Bitmap,species:Species,stage:GrowthStage,motion:PetMotion,
    phase:Float,description:String,modifier:Modifier=Modifier
) {
    Image(bitmap=remember(bitmap){bitmap.asImageBitmap()},contentDescription=description,
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
