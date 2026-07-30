package com.cbgm.securechat.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.securechat.core.ui.scroll.BarsState
import com.cbgm.securechat.core.ui.scroll.rememberBarsState
import com.cbgm.securechat.core.ui.theme.Alpha

@Composable
fun SecureChatLazyScaffold(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    background: @Composable BoxScope.() -> Unit = {},
    topBar: @Composable (containerColor: Color) -> Unit = {},
    bottomBar: @Composable (containerColor: Color) -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        listState: LazyListState,
    ) -> Unit,
) {
    val listState = rememberLazyListState()

    val barsState =
        rememberBarsState(
            state = listState,
            fadedAlpha = fadedAlpha,
        )

    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha),
        label = "SecureChatTopBarColor",
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha),
        label = "SecureChatBottomBarColor",
    )

    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            },
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            snackbarHost = {
                snackbarHostState?.let { state ->
                    SnackbarHost(hostState = state)
                }
            },
        ) { innerPadding ->
            content(
                innerPadding,
                listState,
            )
        }
    }
}

@Composable
fun SecureChatScrollScaffold(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    background: @Composable () -> Unit = {},
    topBar: @Composable (Color) -> Unit = {},
    bottomBar: @Composable (Color) -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        scrollState: ScrollState,
    ) -> Unit,
) {
    val scrollState = rememberScrollState()

    val barsState =
        rememberBarsState(
            state = scrollState,
            fadedAlpha = fadedAlpha,
        )

    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha),
        label = "SecureChatTopBarColor",
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha),
        label = "SecureChatBottomBarColor",
    )

    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            },
        ) { innerPadding ->
            content(
                innerPadding,
                scrollState,
            )
        }
    }
}

@Composable
fun SecureChatStaticScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    background: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

@Stable
data class SecureChatMainScrollStates(
    val chats: LazyListState,
    val identity: ScrollState,
    val settings: ScrollState,
)

enum class SecureChatMainScrollTarget {
    Chats,
    Identity,
    Settings,
}

@Stable
sealed interface SecureChatScrollState {
    @Stable
    data class LazyList(
        val state: LazyListState,
    ) : SecureChatScrollState

    @Stable
    data class Scroll(
        val state: ScrollState,
    ) : SecureChatScrollState
}

@Stable
class SecureChatTabbedScrollStates<T : Any> internal constructor(
    private val states: Map<T, SecureChatScrollState>,
) {
    operator fun get(
        target: T,
    ): SecureChatScrollState =
        requireNotNull(states[target]) {
            "No scroll state registered for target: $target"
        }

    fun lazyListState(
        target: T,
    ): LazyListState =
        when (val scrollState = get(target)) {
            is SecureChatScrollState.LazyList -> {
                scrollState.state
            }

            is SecureChatScrollState.Scroll -> {
                error(
                    "Target $target uses ScrollState, not LazyListState.",
                )
            }
        }

    fun scrollState(
        target: T,
    ): ScrollState =
        when (val scrollState = get(target)) {
            is SecureChatScrollState.Scroll -> {
                scrollState.state
            }

            is SecureChatScrollState.LazyList -> {
                error(
                    "Target $target uses LazyListState, not ScrollState.",
                )
            }
        }
}

enum class SecureChatScrollStateType {
    LazyList,
    Scroll,
}

@Composable
fun <T : Any> rememberSecureChatTabbedScrollStates(
    targets: Map<T, SecureChatScrollStateType>,
): SecureChatTabbedScrollStates<T> {
    require(targets.isNotEmpty()) {
        "At least one scroll target is required."
    }

    require(targets.size <= 5) {
        "SecureChatTabbedScaffold supports at most 5 scroll targets."
    }

    val rememberedStates =
        targets.mapValues { (_, type) ->
            when (type) {
                SecureChatScrollStateType.LazyList -> {
                    SecureChatScrollState.LazyList(
                        state = rememberLazyListState(),
                    )
                }

                SecureChatScrollStateType.Scroll -> {
                    SecureChatScrollState.Scroll(
                        state = rememberScrollState(),
                    )
                }
            }
        }

    return remember(rememberedStates) {
        SecureChatTabbedScrollStates(
            states = rememberedStates,
        )
    }
}

@Composable
fun <T : Any> SecureChatTabbedScaffold(
    selectedScrollTarget: T,
    scrollTargets: Map<T, SecureChatScrollStateType>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    background: @Composable BoxScope.() -> Unit = {},
    topBar: @Composable (containerColor: Color) -> Unit = {},
    bottomBar: @Composable (containerColor: Color) -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        scrollStates: SecureChatTabbedScrollStates<T>,
    ) -> Unit,
) {
    require(selectedScrollTarget in scrollTargets) {
        "Selected target $selectedScrollTarget is not registered."
    }

    val scrollStates =
        rememberSecureChatTabbedScrollStates(
            targets = scrollTargets,
        )

    val barsState =
        when (
            val selectedState =
                scrollStates[selectedScrollTarget]
        ) {
            is SecureChatScrollState.LazyList -> {
                rememberBarsState(
                    state = selectedState.state,
                    fadedAlpha = fadedAlpha,
                )
            }

            is SecureChatScrollState.Scroll -> {
                rememberBarsState(
                    state = selectedState.state,
                    fadedAlpha = fadedAlpha,
                )
            }
        }

    SecureChatTabbedScaffoldContent(
        barsState = barsState,
        barColor = barColor,
        containerColor = containerColor,
        modifier = modifier,
        floatingActionButtonPosition =
        floatingActionButtonPosition,
        background = background,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton =
        floatingActionButton,
    ) { innerPadding ->
        content(
            innerPadding,
            scrollStates,
        )
    }
}

@Composable
private fun SecureChatTabbedScaffoldContent(
    barsState: BarsState,
    barColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    floatingActionButtonPosition: FabPosition,
    background: @Composable BoxScope.() -> Unit,
    topBar: @Composable (containerColor: Color) -> Unit,
    bottomBar: @Composable (containerColor: Color) -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha),
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha),
    )

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            },
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}
