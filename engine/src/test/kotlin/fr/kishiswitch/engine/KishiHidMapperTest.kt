package fr.kishiswitch.engine

import org.junit.Assert.*
import org.junit.Test

class KishiHidMapperTest {
    private fun bits(b:ByteArray)=(b[0].toInt() and 255) or ((b[1].toInt() and 255) shl 8)
    private fun axis(b:ByteArray,i:Int)=b[3+i].toInt() and 255
    @Test fun allEightChordsAndPlainButtons(){
        for(trigger in listOf(9,10))for((from,to) in listOf(304 to 307,307 to 304,305 to 308,308 to 305)){
            val m=KishiHidMapper(.5f,Options(true,false))
            m.key(from,1);m.axis(trigger,200)
            assertEquals(1 shl(to-304),bits(m.frame()))
            m.axis(trigger,0);m.frame();m.key(from,0);assertEquals(0,bits(m.frame()))
            m.key(from,1);assertEquals(1 shl(from-304),bits(m.frame()))
        }
    }
    @Test fun collisionsDoNotReleaseAnotherPhysicalHold(){
        val m=KishiHidMapper(.5f,Options(true,false))
        m.axis(10,255);m.key(304,1);m.frame();m.axis(10,0);m.frame()
        m.key(307,1);m.frame();m.key(304,0)
        assertEquals(1 shl 3,bits(m.frame()))
        m.key(307,0);assertEquals(0,bits(m.frame()))
    }
    @Test fun verticalNeutralEndpointsAndOtherAxes(){
        val m=KishiHidMapper(.5f,Options(false,true))
        assertEquals(127,axis(m.frame(),3)) // 127 and 128 are both in Android's native neutral zone.
        m.axis(0,255);m.axis(2,0);m.axis(5,255);m.axis(9,111)
        var b=m.frame();assertEquals(0,axis(b,3));assertEquals(255,axis(b,0));assertEquals(0,axis(b,2));assertEquals(111,b[8].toInt() and 255)
        m.axis(5,0);b=m.frame();assertEquals(255,axis(b,3))
        m.axis(5,128);assertEquals(127,axis(m.frame(),3))
        m.axis(5,136);assertEquals(119,axis(m.frame(),3))
    }
    @Test fun changesWaitForReleaseAndStickNeutral(){
        val m=KishiHidMapper(.5f,Options(true,true))
        m.axis(10,255);m.axis(5,0);m.key(304,1);m.frame();m.request(Options(false,false))
        assertTrue(m.pending);assertEquals(Options(true,true),m.effective)
        m.axis(5,128);m.frame();assertEquals(Options(true,false),m.effective)
        m.axis(10,0);m.key(304,0);m.frame();assertFalse(m.pending);assertTrue(m.neutral)
    }
    @Test fun hatAndTriggerOnlyReportsAndRepeatedKeys(){
        val m=KishiHidMapper(.5f,Options(false,false))
        m.axis(16,-1);m.axis(17,-1);m.axis(10,97);m.key(304,1);m.key(304,2)
        val b=m.frame();assertEquals(7,b[2].toInt());assertEquals(97,b[7].toInt());assertEquals(1,bits(b));assertFalse(m.neutral)
        m.key(304,0);m.axis(16,0);m.axis(17,0);m.axis(10,0);assertEquals(8,m.frame()[2].toInt());assertTrue(m.neutral)
    }
}
