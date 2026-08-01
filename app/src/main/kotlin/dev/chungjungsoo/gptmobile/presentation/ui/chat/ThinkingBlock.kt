package dev.chungjungsoo.gptmobile.presentation.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.gptmobile.R
import dev.chungjungsoo.gptmobile.presentation.theme.GPTMobileTheme

@Composable
fun ThinkingBlock(
    modifier: Modifier = Modifier,
    thoughts: String,
    contentIdentity: Any = thoughts,
    isLoading: Boolean = false,
    onExpandChange: ((Boolean) -> Unit)? = null
) {
    if (thoughts.isBlank()) return

    var isExpanded by remember { mutableStateOf(false) }
    val currentIsExpanded = isExpanded
    LaunchedEffect(currentIsExpanded) {
        onExpandChange?.invoke(currentIsExpanded)
    }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💭",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isExpanded) {
                    stringResource(R.string.hide_thinking)
                } else {
                    stringResource(R.string.view_thinking)
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                Text(
                    text = stringResource(R.string.thinking_in_progress),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) {
                    stringResource(R.string.collapse)
                } else {
                    stringResource(R.string.expand)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        if (isExpanded) {
            val displayText = if (isLoading) thoughts + "●" else thoughts

            // Cap the expanded thinking content height with an inner scroll,
            // mirroring ChatBox's ReasoningContentUI (maxHeight 400px +
            // overflowY auto). A long chain of thought must not inflate the
            // LazyColumn item to thousands of pixels — that made the outer
            // auto-scroll follow logic jitter against layout changes.
            //
            // The inner scroll follows the same model as the chat list:
            // while the user is not interacting it tracks the bottom (so a
            // streaming chain of thought stays visible); any gesture pauses
            // it; scrolling back to the bottom resumes it.
            val innerScrollState = rememberScrollState()
            var following by remember { mutableStateOf(false) }

            // Expanding settles at the bottom (wait for the first layout).
            LaunchedEffect(isExpanded) {
                if (isExpanded) {
                    following = true
                    while (innerScrollState.maxValue <= 0) {
                        delay(16)
                    }
                    innerScrollState.scrollTo(innerScrollState.maxValue)
                }
            }

            // Streaming growth follows the bottom while following.
            LaunchedEffect(thoughts) {
                if (following && innerScrollState.maxValue > 0) {
                    innerScrollState.scrollTo(innerScrollState.maxValue)
                }
            }

            // Any user gesture pauses following.
            LaunchedEffect(innerScrollState) {
                snapshotFlow { innerScrollState.isScrollInProgress }
                    .collect { inProgress ->
                        if (inProgress) following = false
                    }
            }

            // Gesture ends at (near) the bottom -> resume following.
            LaunchedEffect(innerScrollState) {
                val tolerance = with(LocalDensity.current) { 64.dp.toPx() }.toInt().coerceAtLeast(8)
                snapshotFlow { innerScrollState.isScrollInProgress }
                    .filter { !it }
                    .collect {
                        if (innerScrollState.value >= innerScrollState.maxValue - tolerance) {
                            following = true
                        }
                    }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .heightIn(max = 320.dp)
                    .verticalScroll(innerScrollState)
            ) {
                ChatMarkdown(
                    content = displayText,
                    contentIdentity = contentIdentity,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!isExpanded && thoughts.isNotBlank()) {
            Text(
                text = thoughts.take(100).replace("\n", " ") + if (thoughts.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ThinkingBlockPreview() {
    val sampleThoughts = """
        Let me think about this step by step:
        
        1. First, I need to understand the problem
        2. Then, I'll analyze the requirements
        3. Finally, I'll provide a solution
        
        This is a longer thinking process that shows how the AI reasons through the problem.
    """.trimIndent()

    GPTMobileTheme {
        ThinkingBlock(
            thoughts = sampleThoughts,
            isLoading = false
        )
    }
}

@Preview
@Composable
private fun ThinkingBlockLoadingPreview() {
    GPTMobileTheme {
        ThinkingBlock(
            thoughts = "Analyzing the problem...",
            isLoading = true
        )
    }
}
