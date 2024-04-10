package com.wire.android.feature.sketch

import androidx.compose.ui.geometry.Offset
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingCanvasViewModelTest {

    @Test
    fun givenOnStartDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventDown() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStartDrawing(INITIAL_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Down, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnDrawIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventMove() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onDraw(INITIAL_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Move, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnStopDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventUp() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStopDrawing()

        // then
        assertEquals(DrawingMotionEvent.Up, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenStartDrawingEvent_WhenCallingTheAction_ThenUpdateTheStateWithTheInitialPathPosition() {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPosition, Offset.Unspecified)

        // when
        startDrawing(viewModel)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Down, drawingMotionEvent)
            assertEquals(currentPath.path, paths.first().path)
            assertEquals(currentPosition, MOVED_OFFSET)
        }
    }

    // simulates the start of drawing of strokes
    private fun startDrawing(viewModel: DrawingCanvasViewModel) = with(viewModel) {
        onStartDrawing(MOVED_OFFSET)
        onStartDrawingEvent()
    }

    private class Arrangement {
        val viewModel = DrawingCanvasViewModel()
        fun arrange() = this to viewModel
    }

    private companion object {
        val INITIAL_OFFSET = Offset(0f, 0f)
        val MOVED_OFFSET = Offset(10f, 10f)
    }
}
