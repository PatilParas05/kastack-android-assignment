package dev.paraspatil.luminaai.domain.pipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatPipelineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var chatPipeline: ChatPipeline

    @Before
    fun setup() {
        // Set the main dispatcher to our test dispatcher so we can control time
        Dispatchers.setMain(testDispatcher)
        chatPipeline = ChatPipeline()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage transitions through Happy Path (Typing, Validating, Processing, Responding, Idle)`() = runTest(testDispatcher) {
        val emittedStates = mutableListOf<ChatState>()
        val job = launch {
            chatPipeline.chatState.toList(emittedStates)
        }

        // Trigger the pipeline and pass 'this' (the TestScope) as the CoroutineScope
        chatPipeline.sendMessage("Hello Lumina", this)

        // Advance time to allow the pipeline to complete
        advanceTimeBy(15000)

        // Verify the exact sequence of states
        assertTrue(emittedStates[0] is ChatState.Idle)
        assertTrue(emittedStates[1] is ChatState.Typing)
        assertTrue(emittedStates[2] is ChatState.Validating)
        assertTrue(emittedStates[3] is ChatState.Processing)
        assertTrue(emittedStates[4] is ChatState.Responding)
        assertTrue(emittedStates[5] is ChatState.Idle)

        job.cancel()
    }

    @Test
    fun `sendMessage cancels mid-flow when a new message is sent`() = runTest(testDispatcher) {
        val emittedStates = mutableListOf<ChatState>()
        val job = launch {
            chatPipeline.chatState.toList(emittedStates)
        }

        // Send first message
        chatPipeline.sendMessage("First Message", this)

        // Advance time just enough to be in the "Typing" or "Validating" state, but not finished
        advanceTimeBy(600)

        // Send second message before the first one finishes (Interrupting it)
        chatPipeline.sendMessage("Second Message", this)

        // Advance time to completion
        advanceTimeBy(15000)

        // Count how many times we hit "Typing" - it should be twice because the pipeline restarted!
        val typingStatesCount = emittedStates.count { it is ChatState.Typing }
        assertEquals(2, typingStatesCount)

        job.cancel()
    }

    @Test
    fun `empty message triggers Error state immediately`() = runTest(testDispatcher) {
        val emittedStates = mutableListOf<ChatState>()
        val job = launch {
            chatPipeline.chatState.toList(emittedStates)
        }

        // Send invalid blank message
        chatPipeline.sendMessage("   ", this)
        advanceTimeBy(2000)

        // Verify it went from Idle -> Typing -> Validating -> Error
        assertTrue(emittedStates.last() is ChatState.Error)
        assertEquals("Message cannot be empty", (emittedStates.last() as ChatState.Error).errorMessage)

        job.cancel()
    }
}