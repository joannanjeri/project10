package com.example.project10

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * gesture activity handling gesture inputs and displaying them
 * uses jetpack compose for layout and gesture handling
 */
class GestureActivity : ComponentActivity() {
    /**
     * creates the activity content using jetpack compose
     * sets up the gesture playground and log display
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestureApp()
        }
    }
}

/**
 * main composable function for the gesture activity
 * organizes the layout into a gesture area and a log display
 */
@Composable
fun GestureApp() {
    val gestureLog = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        GesturePlayground(Modifier.weight(1f), gestureLog)
        GestureLog(gestureLog, Modifier.weight(1f))
    }
}

/**
 * composable function representing the gesture playground
 * handles gestures and updates the ball's position based on the gestures
 * logs gesture movements
 * @param modifier modifier for styling and layout
 * @param gestureLog mutable state list for logging gestures
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GesturePlayground(modifier: Modifier, gestureLog: MutableList<String>) {
    val ballPosition = remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFAED581))
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> true
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.x - ballPosition.value.x
                        val deltaY = event.y - ballPosition.value.y
                        val direction = quantizeDirection(deltaX, deltaY)
                        ballPosition.value = ballPosition.value.plus(direction)
                        gestureLog.add("Moved ${directionToString(direction)}")
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Gestures playground",
            color = Color.White,
            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )
        Ball(offset = ballPosition.value)
    }
}

/**
 * composable function to display a ball
 * the ball's position is updated based on user gestures
 * @param offset position of the ball
 */
@Composable
fun Ball(offset: Offset) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(Color.Red, shape = CircleShape)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
    )
}

/**
 * direction of a movement into eight possible directions
 * @param deltaX horizontal movement delta
 * @param deltaY vertical movement delta
 * @return quantized direction as an offset
 */
fun quantizeDirection(deltaX: Float, deltaY: Float): Offset {
    val angle = atan2(deltaY.toDouble(), deltaX.toDouble())
    val step = sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtMost(10.0F)
    return when {
        angle in -Math.PI / 8..Math.PI / 8 -> Offset(step.toFloat(), 0f) // right
        angle in 3 * Math.PI / 8..5 * Math.PI / 8 -> Offset(0f, step.toFloat()) // down
        angle in -5 * Math.PI / 8..-3 * Math.PI / 8 -> Offset(0f, -step.toFloat()) // up
        angle < -7 * Math.PI / 8 || angle > 7 * Math.PI / 8 -> Offset(-step.toFloat(), 0f) // left
        angle in Math.PI / 8..3 * Math.PI / 8 -> Offset(step.toFloat(), step.toFloat()) // bottom right
        angle in -3 * Math.PI / 8..-Math.PI / 8 -> Offset(step.toFloat(), -step.toFloat()) // top right
        angle in 5 * Math.PI / 8..7 * Math.PI / 8 -> Offset(-step.toFloat(), step.toFloat()) // bottom left
        angle in -7 * Math.PI / 8..-5 * Math.PI / 8 -> Offset(-step.toFloat(), -step.toFloat()) // top left
        else -> Offset.Zero
    }
}

/**
 * converts a movement direction into a string representation
 * @param offset direction of movement
 * @return string representation of the direction
 */
fun directionToString(offset: Offset): String {
    return when {
        offset.x > 0 && offset.y == 0f -> "right"
        offset.x == 0f && offset.y > 0 -> "down"
        offset.x == 0f && offset.y < 0 -> "up"
        offset.x < 0 && offset.y == 0f -> "left"
        offset.x > 0 && offset.y > 0 -> "bottom-right"
        offset.x > 0 && offset.y < 0 -> "top-right"
        offset.x < 0 && offset.y > 0 -> "bottom-left"
        offset.x < 0 && offset.y < 0 -> "top-left"
        else -> "nowhere"
    }
}

/**
 * composable function to display a gesture log
 * lists the gestures recorded in the gesture playground
 * @param gestureLog list of logged gestures
 * @param modifier modifier for styling and layout
 */
@Composable
fun GestureLog(gestureLog: List<String>, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Gesture Log")
        gestureLog.forEach { gesture ->
            Text(gesture)
        }
    }
}

/**
 * preview for the gesture playground
 */
@Preview(showBackground = true, name = "Gesture Playground Preview")
@Composable
fun GesturePlaygroundPreview() {
    val mockGestureLog = remember { mutableStateListOf<String>() }
    GesturePlayground(Modifier.fillMaxSize(), mockGestureLog)
}

/**
 * preview for the gesture log
 */
@Preview(showBackground = true, name = "Gesture Log Preview")
@Composable
fun GestureLogPreview() {
    val mockGestureLog = listOf("Swiped right", "Swiped left", "Double tapped")
    GestureLog(mockGestureLog, Modifier.fillMaxSize())
}

/**
 * preview for the entire gesture activity
 */
@Preview(showBackground = true, name = "Gesture Activity Preview")
@Composable
fun GestureActivityPreview() {
    val mockGestureLog = remember { mutableStateListOf<String>() }
    Column(modifier = Modifier.fillMaxSize()) {
        GesturePlayground(Modifier.weight(1f), mockGestureLog)
        GestureLog(mockGestureLog, Modifier.weight(1f))
    }
}

