package fr.kishiswitch.engine

import kotlin.math.abs

/** Linux Kishi V2 Pro reports -> one coherent USB gamepad report per SYN_REPORT. */
class KishiHidMapper(threshold: Float, options: Options, private val flats: IntArray = intArrayOf(15, 15, 15, 15)) {
    companion object {
        val RAW_AXES = intArrayOf(0, 1, 2, 5, 10, 9, 16, 17)
        private val ANDROID_KEYS = intArrayOf(96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 109, 108, 110, 106, 107, 0)
        // Same 8-bit axes as the physical Kishi. Wider axes make Linux add unwanted fuzz.
        val DESCRIPTOR = intArrayOf(5,1,9,5,161,1,5,9,25,1,41,16,21,0,37,1,117,1,149,16,129,2,
            5,1,9,57,21,0,37,7,117,4,149,1,129,66,117,4,149,1,129,3,
            9,48,9,49,9,50,9,53,5,2,9,197,9,196,21,0,38,255,0,117,8,149,6,129,2,192)
    }
    private val raw = intArrayOf(128,128,128,128,0,0,0,0)
    private val held = mutableMapOf<Int, Int>()
    private val changes = mutableListOf<Pair<Int, Int>>()
    private val engine = RemapEngine(AxisSpec(14,-1f,1f), TriggerSpec(17), TriggerSpec(18), threshold, options)
    var frames = 0L; private set
    var buttonChanges = 0L; private set
    val effective get() = engine.effective
    val pending get() = engine.hasPendingOptions
    val neutral get() = held.isEmpty() && raw.take(4).withIndex().all { abs(it.value-128) <= flats[it.index] } && raw[4] == 0 && raw[5] == 0 && raw[6] == 0 && raw[7] == 0

    fun request(options: Options) = engine.requestOptions(options)
    fun axis(code: Int, value: Int) {
        val index = RAW_AXES.indexOf(code)
        require(index >= 0) { "Axe physique non pris en charge : $code" }
        raw[index] = if (index < 6) value.coerceIn(0,255) else value.coerceIn(-1,1)
    }
    fun key(code: Int, value: Int) {
        // The extra vendor keys 0x13f/0x2c0/0x2c1 have no mapping in this phone's Generic.kl.
        if (code in 304..318 && value != 2) changes.add(code to value)
    }
    private fun normalized(index: Int): Float {
        val v = raw[index]-128
        if (abs(v) <= flats[index]) return 0f
        return if (v < 0) v/128f else v/127f
    }
    fun frame(): ByteArray {
        engine.observeMotion(mapOf(17 to raw[4]/255f,18 to raw[5]/255f,14 to normalized(3)))
        // Analog values from the complete frame must precede the simultaneous button decisions.
        for ((code,value) in changes) {
            val android = ANDROID_KEYS[code-304]
            if (value == 0) { engine.keyUp(android); held.remove(code) }
            else held[code] = engine.keyDown(android).logical
            buttonChanges++
        }
        changes.clear(); frames++
        return currentReport()
    }
    fun currentReport(): ByteArray {
        var bits=0
        held.values.forEach { logical -> val index=ANDROID_KEYS.indexOf(logical); if(index>=0) bits=bits or (1 shl index) }
        val x=raw[6]; val y=raw[7]
        val hat=when { y<0 -> if(x<0)7 else if(x>0)1 else 0; y>0 -> if(x<0)5 else if(x>0)3 else 4; x<0->6; x>0->2; else->8 }
        return ByteArray(9).apply {
            this[0]=bits.toByte();this[1]=(bits shr 8).toByte();this[2]=hat.toByte()
            for(i in 0..3){
                this[3+i]=(if(i==3 && effective.rightVertical)255-raw[i] else raw[i]).toByte()
            }
            this[7]=raw[4].toByte();this[8]=raw[5].toByte()
        }
    }
}
