package com.remgose.rem.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.remgose.rem.R
import com.remgose.rem.data.GameEntity
import java.io.File

/** Logo Rem Gose - avatar cua Rem, dung o header, man hinh trong va Cai dat. */
@Composable
fun RemGoseLogo(size: Int = 44, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.rem_gose_logo),
        contentDescription = "Logo Rem Gose",
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size.dp).clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
    )
}

/**
 * Man hinh Thu vien - man hinh chinh cua Rem Gose.
 * Header gradient + loi chao, o tim kiem, luoi the game 2 cot:
 * anh bia lon, ten game, badge phien ban Ren'Py + trang thai runtime,
 * nut Choi noi (gradient) goc phai duoi moi the.
 */
@Composable
fun LibraryScreen(
    games: List<GameEntity>,
    onImportClick: () -> Unit,
    onGameClick: (GameEntity) -> Unit,
    onPlayClick: (GameEntity) -> Unit,
    onSettingsClick: () -> Unit,
) {
    // Tim kiem nhu RenDroid (icon kinh lup o header)
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) games else games.filter {
        (it.displayName ?: it.name).contains(query, ignoreCase = true)
    }
    Box(Modifier.fillMaxSize().background(RemGoseColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            LibraryHeader(onSettingsClick)
            if (games.isNotEmpty()) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text(LocalStrings.current.searchGames) }, singleLine = true,
                    leadingIcon = { Text("🔍") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                )
                if (filtered.isEmpty()) {
                    Text(LocalStrings.current.noResultsFor.format(query),
                        color = RemGoseColors.TextMuted, modifier = Modifier.padding(16.dp))
                }
            }
            if (games.isEmpty()) EmptyLibrary(onImportClick)
            else LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { items(filtered, key = { it.name }) { GameCard(it, onGameClick, onPlayClick) } }
        }
        // Nut Import co chu nhu RenDroid ("+ Import"), nhung theo mau gradient Rem Gose, khong quang cao
        ExtendedFloatingActionButton(
            onClick = onImportClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = RemGoseColors.AccentViolet, contentColor = Color.White,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(LocalStrings.current.importLabel, fontWeight = FontWeight.SemiBold) },
        )
    }
}

@Composable
private fun LibraryHeader(onSettingsClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(RemGoseColors.BrandGradient).padding(20.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemGoseLogo(46)
                    Spacer(Modifier.width(12.dp))
                    Text("Rem Gose", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = LocalStrings.current.settings, tint = Color.White)
                }
            }
            Text(LocalStrings.current.appTagline, color = Color(0xE6FFFFFF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun GameCard(game: GameEntity, onClick: (GameEntity) -> Unit, onPlay: (GameEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(game) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RemGoseColors.Surface),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(RemGoseColors.SurfaceVariant)) {
                if (game.icon != null) {
                    AsyncImage(model = File(game.icon), contentDescription = game.displayName,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    // Game khong co icon: hien chu cai dau nhu RenDroid (vd "G" cho Goddesses_Whim)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((game.displayName ?: game.name).trim().firstOrNull()?.uppercase() ?: "?",
                            fontSize = 52.sp, fontWeight = FontWeight.Bold, color = RemGoseColors.Accent)
                    }
                }
                StatusBadge(game, Modifier.align(Alignment.TopStart).padding(8.dp))
                if (game.statusPhase == "READY") {
                    FloatingActionButton(onClick = { onPlay(game) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp),
                        shape = CircleShape, containerColor = RemGoseColors.Accent, contentColor = Color.White,
                    ) { Icon(Icons.Filled.PlayArrow, contentDescription = LocalStrings.current.play) }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(game.displayName ?: game.name, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = RemGoseColors.TextPrimary)
                Text(
                    "Ren'Py ${game.renPyVersion ?: "?"} · ${game.runtimeVersionOverride ?: LocalStrings.current.runtimeAuto}",
                    fontSize = 12.sp, color = RemGoseColors.TextMuted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (game.statusPhase == "IMPORTING" || game.statusPhase == "ERROR") {
                    Text(game.statusMessage ?: game.statusPhase, fontSize = 12.sp,
                        color = if (game.statusPhase == "ERROR") RemGoseColors.Error else RemGoseColors.Warning,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (game.statusPhase == "IMPORTING") LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(game: GameEntity, modifier: Modifier = Modifier) {
    val (label, color) = when (game.statusPhase) {
        "READY" -> LocalStrings.current.statusReady to RemGoseColors.Success
        "IMPORTING" -> LocalStrings.current.statusImporting to RemGoseColors.Warning
        "ERROR" -> LocalStrings.current.statusError to RemGoseColors.Error
        "RUNNING" -> LocalStrings.current.statusRunning to RemGoseColors.AccentPink
        else -> game.statusPhase to RemGoseColors.TextMuted
    }
    Box(modifier.clip(RoundedCornerShape(50)).background(Color(0xCC0B0B14)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyLibrary(onImportClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        RemGoseLogo(104)
        Spacer(Modifier.height(16.dp))
        Text(LocalStrings.current.libraryEmptyTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = RemGoseColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(LocalStrings.current.libraryEmptySub,
            color = RemGoseColors.TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onImportClick, colors = ButtonDefaults.buttonColors(containerColor = RemGoseColors.AccentViolet)) {
            Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(LocalStrings.current.importFirstGame)
        }
    }
}
