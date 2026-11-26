package com.pixubi.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class SoundSettings(
    val music: Boolean = true,
    val effects: Boolean = true,
    val musicVolume: Float = 0.28f,
    val effectsVolume: Float = 0.60f
)

class SoundSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_audio_v1", Context.MODE_PRIVATE)
    fun load(): SoundSettings = SoundSettings(
        music = prefs.getBoolean("music", true),
        effects = prefs.getBoolean("effects", true),
        musicVolume = prefs.getFloat("music_volume", 0.28f).coerceIn(0f, 1f),
        effectsVolume = prefs.getFloat("effects_volume", 0.60f).coerceIn(0f, 1f)
    )
    fun save(value: SoundSettings) {
        prefs.edit().putBoolean("music", value.music).putBoolean("effects", value.effects)
            .putFloat("music_volume", value.musicVolume.coerceIn(0f,1f))
            .putFloat("effects_volume", value.effectsVolume.coerceIn(0f,1f)).apply()
    }
}

enum class PixubiSound { TAP, FEED, WATER, SLEEP, WAKE, FREEZE, STAR, HAPPY, QUIZ_CORRECT, QUIZ_WRONG }

/**
 * Original layered instrumental score and digital effects.
 * No audio files, no copyright songs, no INTERNET permission and no background playback.
 */
class PixubiAudio(context: Context) {
    private val rate = 22_050
    private val soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val effectIds = mutableMapOf<PixubiSound, Int>()
    @Volatile private var options = SoundSettings()
    @Volatile private var region = Region.FOREST
    @Volatile private var scene = MusicScene.BASE
    @Volatile private var foreground = false
    @Volatile private var closed = false
    private val backgroundThread: Thread

    init {
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) loaded.add(id)
        }
        val soundDir = File(context.cacheDir, "pixubi-synth").apply { mkdirs() }
        PixubiSound.entries.forEach { effect ->
            val wav = File(soundDir, effect.name.lowercase() + ".wav")
            if (!wav.exists() || wav.length() < 100) wav.writeBytes(createEffectWav(effect))
            effectIds[effect] = soundPool.load(wav.absolutePath, 1)
        }
        backgroundThread = Thread({ musicLoop() }, "PixubiMusic").apply {
            isDaemon = true
            start()
        }
    }

    fun configure(next: SoundSettings, world: Region) {
        options = next
        region = world
    }

    fun setMusicScene(value: MusicScene) { scene = value }

    fun setForeground(value: Boolean) { foreground = value }

    fun effect(which: PixubiSound) {
        val current = options
        if (!foreground || closed || !current.effects) return
        val id = effectIds[which] ?: return
        if (id !in loaded) return
        val vol = current.effectsVolume.coerceIn(0f, 1f)
        soundPool.play(id, vol, vol, 1, 0, 1f)
    }

    fun close() {
        if (closed) return
        closed = true
        foreground = false
        backgroundThread.interrupt()
        soundPool.release()
    }

    private fun makeTrack(): AudioTrack? = runCatching {
        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT)
        val size = max(8192, minBytes.coerceAtLeast(0) * 2)
        AudioTrack.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        ).setAudioFormat(audioFormat).setBufferSizeInBytes(size)
            .setTransferMode(AudioTrack.MODE_STREAM).build().also {
                if (it.state != AudioTrack.STATE_INITIALIZED) {
                    it.release()
                    throw IllegalStateException("Audio output unavailable")
                }
            }
    }.getOrNull()

    private fun musicLoop() {
        var player: AudioTrack? = null
        var active: Pair<Region, MusicScene>? = null
        var position = 0
        var fade = 0f
        var song: ShortArray? = null
        val cache = object : LinkedHashMap<Pair<Region,MusicScene>,ShortArray>(4,.75f,true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Region,MusicScene>,ShortArray>?): Boolean = size > 3
        }
        val samples = ShortArray(1024)
        try {
            while (!closed) {
                if (!foreground || !options.music) {
                    player?.let { runCatching { it.pause();it.flush();it.release() } }
                    player=null
                    Thread.sleep(120)
                    continue
                }
                val desired=region to scene
                if(active!=desired || song==null){
                    // Fade old music before changing the arrangement.
                    if(player!=null && song!=null && fade>0f){
                        fade=(fade-.23f).coerceAtLeast(0f)
                    }else{
                        player?.let{runCatching{it.pause();it.flush();it.release()}}
                        player=null
                        song=cache[desired]?:PixubiScore.render(desired.first,desired.second,rate).also{cache[desired]=it}
                        active=desired;position=0;fade=0f
                        if(!foreground||!options.music||closed||desired!=(region to scene))continue
                    }
                }else fade=(fade+.075f).coerceAtMost(1f)
                val pcm=song?:continue
                if(player==null){
                    player=makeTrack()
                    if(player==null){Thread.sleep(600);continue}
                    player.setVolume(0f);player.play()
                }
                for(i in samples.indices){
                    samples[i]=pcm[position]
                    position=(position+1)%pcm.size
                }
                player.setVolume(options.musicVolume.coerceIn(0f,1f)*fade*.85f)
                val written=player.write(samples,0,samples.size)
                if(written<0){
                    runCatching{player?.release()}
                    player=null;fade=0f
                }
            }
        } catch (_: InterruptedException) {
            // Release on activity disposal.
        } catch (_: Exception) {
            // Audio must never crash the game.
        } finally {
            player?.let { runCatching { it.pause();it.flush();it.release() } }
        }
    }

    private fun createEffectWav(effect: PixubiSound): ByteArray {
        val preset = when (effect) {
            PixubiSound.TAP -> Triple(540.0, 800.0, .10)
            PixubiSound.FEED -> Triple(520.0, 950.0, .19)
            PixubiSound.WATER -> Triple(750.0, 370.0, .27)
            PixubiSound.SLEEP -> Triple(590.0, 230.0, .37)
            PixubiSound.WAKE -> Triple(350.0, 860.0, .27)
            PixubiSound.FREEZE -> Triple(650.0, 170.0, .40)
            PixubiSound.STAR -> Triple(680.0, 1200.0, .15)
            PixubiSound.HAPPY -> Triple(650.0, 1050.0, .28)
            PixubiSound.QUIZ_CORRECT -> Triple(500.0, 1300.0, .30)
            PixubiSound.QUIZ_WRONG -> Triple(330.0, 220.0, .23)
        }
        val length = (preset.third * rate).toInt()
        val bb = ByteBuffer.allocate(44 + length * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(36 + length * 2)
        bb.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16)
        bb.putShort(1.toShort())
        bb.putShort(1.toShort())
        bb.putInt(rate)
        bb.putInt(rate * 2)
        bb.putShort(2.toShort())
        bb.putShort(16.toShort())
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(length * 2)
        var phase = 0.0
        for (i in 0 until length) {
            val x = i.toDouble() / length
            val frequency = preset.first + (preset.second - preset.first) * x
            phase += 2.0 * PI * frequency / rate
            val envelope = sin(PI * x).coerceAtLeast(0.0).pow(0.65)
            val pluck = if (effect == PixubiSound.WATER) sin(phase) * .8 +
                sin(phase * .5) * .15 else sin(phase) * .82 +
                sin(phase * 2.0) * .17
            val sample = (pluck * envelope * 0.35 * 32767).toInt()
                .coerceIn(-32768,32767).toShort()
            bb.putShort(sample)
        }
        return bb.array()
    }
}

/** Distinct original 16-step themes: forest, peaks, lake, desert, and sky. */
internal data class MelodyTheme(
    val bpm: Int, val notes: IntArray, val bass: IntArray,
    val color: Int, val decay: Double
)

internal object RegionMelodies {
    fun forRegion(region: Region): MelodyTheme = when (region) {
        Region.FOREST -> MelodyTheme(88,
            intArrayOf(67, 72, 76, -1, 74, 72, 67, -1, 64, 67, 72, 74, 76, 72, 67, -1),
            intArrayOf(48, 53, 45, 48), 1, 1.45)
        Region.MOUNTAINS -> MelodyTheme(112,
            intArrayOf(69, 73, 76, 81, 80, 76, 73, 76, 69, 73, 76, 83, 81, 76, 73, 69),
            intArrayOf(45, 50, 47, 45), 2, 1.08)
        Region.LAKES -> MelodyTheme(76,
            intArrayOf(62, -1, 69, 74, 72, -1, 69, 67, 65, -1, 69, 72, 74, -1, 69, 62),
            intArrayOf(50, 46, 48, 50), 0, 1.85)
        Region.DESERT -> MelodyTheme(101,
            intArrayOf(64, 67, 69, 71, 74, 71, 69, 67, 64, -1, 67, 69, 76, 74, 71, -1),
            intArrayOf(40, 45, 43, 40), 3, 1.34)
        Region.SKY -> MelodyTheme(80,
            intArrayOf(72, -1, 76, -1, 79, 81, -1, 79, 76, -1, 74, 76, 79, -1, 72, -1),
            intArrayOf(48, 53, 45, 48), 4, 2.20)
    }
}
