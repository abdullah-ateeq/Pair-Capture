package com.example.paircapture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paircapture.data.AppSettings
import com.example.paircapture.game.GameEventType
import com.example.paircapture.game.GameUiState
import com.example.paircapture.game.PairCaptureViewModel
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.ui.components.CaptureTray
import com.example.paircapture.ui.components.EventLine
import com.example.paircapture.ui.components.GameBoard
import com.example.paircapture.ui.components.ScoreBoard
import com.example.paircapture.ui.components.TurnIndicator
import com.example.paircapture.util.GameSound
import com.example.paircapture.util.HapticKind
import com.example.paircapture.util.SoundPlayer
import com.example.paircapture.util.performGameHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: PairCaptureViewModel,
    settings: AppSettings,
    onExit: () -> Unit,
    onFinished: (MatchSummary) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSettings by rememberUpdatedState(settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    val soundPlayer = remember { SoundPlayer() }
    DisposableEffect(soundPlayer) { onDispose { soundPlayer.release() } }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val s = currentSettings
            val sound = when (event.type) {
                GameEventType.REVEAL_FIRST, GameEventType.REVEAL_SECOND -> GameSound.REVEAL
                GameEventType.MATCH -> GameSound.MATCH
                GameEventType.MISMATCH -> GameSound.MISMATCH
                GameEventType.CPU_TURN -> GameSound.CPU_TURN
                GameEventType.PERFECT, GameEventType.VICTORY -> GameSound.VICTORY
                GameEventType.DEFEAT -> GameSound.DEFEAT
                GameEventType.DRAW -> null
            }
            if (s.sound && sound != null) soundPlayer.play(sound)

            if (s.haptics && event.side == PlayerSide.PLAYER) {
                val haptic = when (event.type) {
                    GameEventType.REVEAL_FIRST, GameEventType.REVEAL_SECOND, GameEventType.MISMATCH -> HapticKind.LIGHT
                    GameEventType.MATCH -> HapticKind.MEDIUM
                    GameEventType.PERFECT -> HapticKind.SUCCESS
                    else -> null
                }
                haptic?.let { view.performGameHaptic(it) }
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(state.summary) {
        if (state.summary != null) viewModel.consumeResult()?.let(onFinished)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PAIR CAPTURE · ${state.difficulty.label.uppercase()}",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (settings.confirmRestart) confirmRestart = true else viewModel.restart()
                        },
                        enabled = state.canRestart
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart match")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val wide = maxWidth > maxHeight || maxWidth >= 840.dp
            if (wide) {
                val boardSize = minOf(maxHeight, maxWidth * 0.58f)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Board(state, settings, viewModel, boardSize)
                    Column(
                        modifier = Modifier.widthIn(min = 240.dp, max = 380.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Hud(state, settings, Modifier.fillMaxWidth())
                    }
                }
            } else {
                // Reserve room for the scoreboard and status rows; the board takes the rest.
                val boardSize = minOf(maxWidth, maxHeight - 260.dp, 560.dp).coerceAtLeast(240.dp)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreBoard(
                        playerPairs = state.playerPairs,
                        cpuPairs = state.cpuPairs,
                        pairsRemaining = state.pairsRemaining,
                        currentSide = state.currentSide,
                        animations = settings.animations,
                        modifier = Modifier.widthIn(max = 560.dp)
                    )
                    Board(state, settings, viewModel, boardSize)
                    TurnIndicator(
                        currentSide = state.currentSide,
                        cpuThinking = state.cpuThinking,
                        selectedCount = state.selectedCount,
                        turnCount = state.turnCount,
                        showTurnCounter = settings.showTurnCounter,
                        gameOver = state.phase == GamePhase.GAME_OVER
                    )
                    CaptureTray(state.playerPairs, state.cpuPairs)
                    EventLine(state.lastEvent, state.playerStreak, settings.animations)
                }
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Restart match?") },
            text = { Text("The current board will be reshuffled. This match won't count toward your statistics.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    viewModel.restart()
                }) { Text("RESTART") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun Board(state: GameUiState, settings: AppSettings, viewModel: PairCaptureViewModel, size: Dp) {
    GameBoard(
        tiles = state.tiles,
        feedback = state.feedback,
        currentSide = state.currentSide,
        enabled = state.boardEnabled,
        animations = settings.animations,
        onTileClick = viewModel::selectTile,
        modifier = Modifier.size(size)
    )
}

@Composable
private fun Hud(state: GameUiState, settings: AppSettings, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScoreBoard(
            playerPairs = state.playerPairs,
            cpuPairs = state.cpuPairs,
            pairsRemaining = state.pairsRemaining,
            currentSide = state.currentSide,
            animations = settings.animations
        )
        Spacer(Modifier.height(16.dp))
        TurnIndicator(
            currentSide = state.currentSide,
            cpuThinking = state.cpuThinking,
            selectedCount = state.selectedCount,
            turnCount = state.turnCount,
            showTurnCounter = settings.showTurnCounter,
            gameOver = state.phase == GamePhase.GAME_OVER
        )
        Spacer(Modifier.height(16.dp))
        CaptureTray(state.playerPairs, state.cpuPairs)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth()) {
            EventLine(state.lastEvent, state.playerStreak, settings.animations)
        }
    }
}
