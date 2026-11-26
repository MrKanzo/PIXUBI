package com.pixubi.game

import kotlin.math.*

/** Original offline score. All note events are authored here, not sampled from songs. */
enum class MusicScene { BASE, EXPEDITION, BATTLE }

internal object PixubiScore {
    private enum class Instrument { PIANO, STRINGS, CELLO, PAD, DRUM }
    private data class Event(val start:Double,val length:Double,val note:Int,val gain:Double,val kind:Instrument)
    private data class Score(val bpm:Int,val events:List<Event>)
    private const val bars=16
    private const val beats=bars*4
    private const val tableSize=2048
    private val wave=DoubleArray(tableSize){sin(2*PI*it/tableSize)}
    private fun osc(p:Double):Double {
        val x=p*tableSize
        val n=x.toInt()
        val f=x-n
        return wave[n and (tableSize-1)]*(1-f)+wave[(n+1) and (tableSize-1)]*f
    }
    private val chords=arrayOf(
        intArrayOf(57,60,64,71), intArrayOf(53,57,60,64),
        intArrayOf(48,55,60,64), intArrayOf(55,59,62,69),
        intArrayOf(57,60,64,71), intArrayOf(53,57,60,64),
        intArrayOf(50,53,57,64), intArrayOf(52,56,59,62),
        intArrayOf(53,57,60,64), intArrayOf(55,59,62,67),
        intArrayOf(52,55,59,64), intArrayOf(57,60,64,69),
        intArrayOf(50,53,57,60), intArrayOf(53,57,60,64),
        intArrayOf(52,56,59,64), intArrayOf(57,60,64,69)
    )
    // A four-phrase melody with breathing space and a complete cadence.
    private val melody=arrayOf(
        intArrayOf(76,72,69,-1),intArrayOf(72,74,76,-1),
        intArrayOf(79,76,74,72),intArrayOf(71,74,72,-1),
        intArrayOf(76,81,79,76),intArrayOf(77,76,72,-1),
        intArrayOf(74,77,76,72),intArrayOf(71,68,71,-1),
        intArrayOf(77,81,84,81),intArrayOf(79,74,71,-1),
        intArrayOf(76,79,76,71),intArrayOf(72,76,81,-1),
        intArrayOf(77,74,72,69),intArrayOf(72,76,77,-1),
        intArrayOf(76,74,71,68),intArrayOf(69,72,76,-1)
    )
    private fun arrangement(region:Region,scene:MusicScene):Score{
        val tempo=when(scene){MusicScene.BASE->when(region){Region.FOREST->78;Region.MOUNTAINS->84;Region.LAKES->68;Region.DESERT->88;Region.SKY->74};MusicScene.EXPEDITION->94;MusicScene.BATTLE->126}
        val shift=when(region){Region.FOREST->0;Region.MOUNTAINS->2;Region.LAKES->-3;Region.DESERT->-2;Region.SKY->5}
        val events=buildList{
            for(bar in 0 until bars){
                val chord=chords[bar];val time=bar*4.0
                val combat=scene==MusicScene.BATTLE
                val exploring=scene==MusicScene.EXPEDITION
                val base=chord[0]+shift
                for(i in chord.indices){
                    add(Event(time,4.15,chord[i]+shift+(if(i==0)0 else 12),if(combat).032 else .039,Instrument.PAD))
                }
                add(Event(time,if(combat)1.4 else 3.75,base-12,if(combat).18 else .11,Instrument.CELLO))
                if(combat||exploring)add(Event(time+2,1.65,base-5,.11,Instrument.CELLO))
                for(step in 0 until (if(combat||exploring)8 else 4)){
                    val i=if(step<4)step else 7-step
                    val note=chord[i]+shift+12
                    add(Event(time+step*(if(combat||exploring).5 else 1.0),if(combat).39 else .85,note,if(combat).065 else .09,Instrument.PIANO))
                }
                for(i in 0..3){
                    val note=melody[bar][i]
                    if(note<0)continue
                    val len=if(i==2&&melody[bar][3]<0)1.8 else .88
                    add(Event(time+i,len,note+shift,if(combat).15 else .19,Instrument.PIANO))
                    if(combat||bar>=8)add(Event(time+i,len,note+shift-12,if(combat).10 else .054,Instrument.STRINGS))
                }
                if(exploring||combat){
                    for(i in 0..3)if(combat||i%2==0)add(Event(time+i,.34,36,if(combat).19 else .08,Instrument.DRUM))
                    if(combat)for(i in listOf(1.5,3.5))add(Event(time+i,.19,48,.075,Instrument.DRUM))
                }
            }
        }
        return Score(tempo,events)
    }
    fun render(region:Region,scene:MusicScene,rate:Int):ShortArray{
        require(rate in 8000..48000)
        val score=arrangement(region,scene)
        val unit=60.0/score.bpm
        val frames=(beats*unit*rate).roundToInt()
        val mix=FloatArray(frames)
        for(e in score.events){
            if(Thread.currentThread().isInterrupted)throw InterruptedException()
            val start=(e.start*unit*rate).roundToInt()
            val duration=e.length*unit
            val release=when(e.kind){Instrument.PAD->.65;Instrument.STRINGS->.28;Instrument.PIANO->.5;Instrument.CELLO->.2;Instrument.DRUM->.02}
            val count=((duration+release)*rate).roundToInt()
            val hz=440.0*2.0.pow((e.note-69)/12.0)
            val attack=when(e.kind){Instrument.PAD->.32;Instrument.STRINGS->.09;Instrument.CELLO->.07;else->.008}
            val step=hz/rate
            var noise=(e.note*9817+start+1)
            for(i in 0 until count){
                val t=i.toDouble()/rate
                val p=i*step
                val env=min(1.0,t/attack)*if(t<=duration)1.0 else ((duration+release-t)/release).coerceIn(0.0,1.0)
                val sample=when(e.kind){
                    Instrument.PIANO->(osc(p)*.68+osc(p*2.003)*.22*exp(-t*3)+osc(p*3.01)*.10*exp(-t*7))*exp(-t*1.75)
                    Instrument.STRINGS->(osc(p)*.48+osc(p*2.0)*.21+osc(p*3.0)*.08+osc(p*1.004)*.23)*(.90+.10*osc(t*4.9))
                    Instrument.CELLO->osc(p)*.65+osc(p*2)*.22+osc(p*3)*.09+osc(p*4)*.04
                    Instrument.PAD->osc(p)*.50+osc(p*1.003)*.24+osc(p*.997)*.18+osc(p*2)*.08
                    Instrument.DRUM->{
                        noise=noise*1664525+1013904223
                        val n=(noise ushr 16)/32768.0-1.0
                        (osc(45*t+4*(1-exp(-t*18)))*.86+n*.14)*exp(-t*14)
                    }
                }
                val at=(start+i)%frames
                mix[at]=(mix[at]+sample*env*e.gain).toFloat()
            }
        }
        // Circular echo gives continuous tails at loop boundaries, no abrupt cuts.
        val d1=(rate*.113).toInt();val d2=(rate*.237).toInt();val d3=(rate*.389).toInt()
        return ShortArray(frames){i->
            val v=mix[i]+.16*mix[(i-d1+frames)%frames]+.11*mix[(i-d2+frames)%frames]+.07*mix[(i-d3+frames)%frames]
            (tanh(v*1.65)*30000).toInt().toShort()
        }
    }
}
