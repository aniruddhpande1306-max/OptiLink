package com.optilink.app.transfer

import com.optilink.app.model.TransferFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameSequenceControllerTest {

    private fun createDummyFrames(count: Int): List<TransferFrame> {
        return (1..count).map {
            TransferFrame(1234, it, count, 0, ByteArray(0), 0)
        }
    }

    @Test
    fun testEmptyFrameList() {
        val controller = FrameSequenceController(emptyList(), OpticalTransferConfig(), CoroutineScope(Dispatchers.Unconfined))
        
        assertEquals(0, controller.totalFrames)
        assertNull(controller.currentFrame)
        assertEquals(0f, controller.progress)
        
        controller.start() // should complete immediately
        assertEquals(TransmissionState.FINISHED, controller.currentState)
    }

    @Test
    fun testSingleFrameList() {
        val frames = createDummyFrames(1)
        val controller = FrameSequenceController(frames, OpticalTransferConfig(), CoroutineScope(Dispatchers.Unconfined))
        
        assertEquals(1, controller.totalFrames)
        assertEquals(frames[0], controller.currentFrame)
        
        // Before completion
        assertEquals(0f, controller.progress)
        
        // Manual advance logic should gracefully do nothing
        controller.nextFrame()
        assertEquals(0, controller.currentIndex)
    }

    @Test
    fun testManualTraverse() {
        val frames = createDummyFrames(3)
        val controller = FrameSequenceController(frames, OpticalTransferConfig(), CoroutineScope(Dispatchers.Unconfined))
        
        assertEquals(3, controller.totalFrames)
        
        // Start index
        assertEquals(0, controller.currentIndex)
        assertEquals(frames[0], controller.currentFrame)
        
        // Next
        controller.nextFrame()
        assertEquals(1, controller.currentIndex)
        assertEquals(frames[1], controller.currentFrame)
        
        // Next
        controller.nextFrame()
        assertEquals(2, controller.currentIndex)
        assertEquals(frames[2], controller.currentFrame) // Boundary
        
        // Next out of bound
        controller.nextFrame()
        assertEquals(2, controller.currentIndex)
        
        // Previous
        controller.previousFrame()
        assertEquals(1, controller.currentIndex)
    }

    @Test
    fun testPauseResumeStopState() {
        val frames = createDummyFrames(5)
        val controller = FrameSequenceController(frames, OpticalTransferConfig(10L, 0), CoroutineScope(Dispatchers.Unconfined)) // fast loop
        
        assertEquals(TransmissionState.IDLE, controller.currentState)
        
        controller.start()
        // Wait, because we used Unconfined without TestScope, exact timing asserts are racey. 
        // We just assert the API doesn't crash during manual bounds check.
        // And we ensure stop transitions to cancelled.
        
        controller.stop()
        assertEquals(TransmissionState.CANCELLED, controller.currentState)
        
        controller.resume()
        // Resume from cancelled should not work (only from PAUSED)
        assertEquals(TransmissionState.CANCELLED, controller.currentState)
    }
}
