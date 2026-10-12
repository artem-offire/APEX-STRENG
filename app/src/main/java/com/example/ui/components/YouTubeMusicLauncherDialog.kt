package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

enum class MusicServiceType(val id: String, val displayName: String, val brandColor: Color) {
    YOUTUBE_MUSIC("YOUTUBE_MUSIC", "YouTube Music", Color(0xFFFF0033)),
    SPOTIFY("SPOTIFY", "Spotify", Color(0xFF1DB954))
}

data class WorkoutPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val queryOrUrl: String,
    val badge: String,
    val letter: String = title.firstOrNull()?.uppercase() ?: "M",
    val accentHex: Long = 0xFF2EE5B5
)

val YOUTUBE_MUSIC_PLAYLISTS = listOf(
    WorkoutPlaylist(
        id = "yt_my_mix",
        title = "Мій мікс",
        subtitle = "Збережене посилання",
        emoji = "M",
        queryOrUrl = "https://music.youtube.com",
        badge = "Мій",
        letter = "M",
        accentHex = 0xFF2EE5B5
    ),
    WorkoutPlaylist(
        id = "yt_heavy_rock",
        title = "Rock & Metal",
        subtitle = "Важкий драйв для максимальних ваг",
        emoji = "R",
        queryOrUrl = "https://music.youtube.com/search?q=workout+heavy+metal+rock+motivation",
        badge = "Сила",
        letter = "R",
        accentHex = 0xFFF59E0B
    ),
    WorkoutPlaylist(
        id = "yt_gym_phonk",
        title = "Gym Phonk",
        subtitle = "Швидкий темп для робочих підходів",
        emoji = "P",
        queryOrUrl = "https://music.youtube.com/search?q=gym+phonk+workout+motivation",
        badge = "Інтенсив",
        letter = "P",
        accentHex = 0xFFF87171
    ),
    WorkoutPlaylist(
        id = "yt_gym_hiphop",
        title = "Hip-Hop & Trap",
        subtitle = "Ритм для об'ємної роботи",
        emoji = "G",
        queryOrUrl = "https://music.youtube.com/search?q=gym+workout+hip+hop+rap",
        badge = "Памп",
        letter = "G",
        accentHex = 0xFFA78BFA
    ),
    WorkoutPlaylist(
        id = "yt_synth_focus",
        title = "Synthwave Focus",
        subtitle = "Рівний темп для концентрації",
        emoji = "S",
        queryOrUrl = "https://music.youtube.com/search?q=workout+synthwave+cyberpunk+focus",
        badge = "Фокус",
        letter = "S",
        accentHex = 0xFF60A5FA
    )
)

val SPOTIFY_PLAYLISTS = listOf(
    WorkoutPlaylist(
        id = "sp_my_mix",
        title = "Мій мікс",
        subtitle = "Збережене посилання",
        emoji = "M",
        queryOrUrl = "https://open.spotify.com",
        badge = "Мій",
        letter = "M",
        accentHex = 0xFF2EE5B5
    ),
    WorkoutPlaylist(
        id = "sp_heavy_rock",
        title = "Rock & Metal",
        subtitle = "Важкий драйв для максимальних ваг",
        emoji = "R",
        queryOrUrl = "https://open.spotify.com/search/workout%20metal%20rock",
        badge = "Сила",
        letter = "R",
        accentHex = 0xFFF59E0B
    ),
    WorkoutPlaylist(
        id = "sp_gym_phonk",
        title = "Gym Phonk",
        subtitle = "Швидкий темп для робочих підходів",
        emoji = "P",
        queryOrUrl = "https://open.spotify.com/search/gym%20phonk",
        badge = "Інтенсив",
        letter = "P",
        accentHex = 0xFFF87171
    ),
    WorkoutPlaylist(
        id = "sp_gym_hiphop",
        title = "Hip-Hop & Trap",
        subtitle = "Ритм для об'ємної роботи",
        emoji = "G",
        queryOrUrl = "https://open.spotify.com/search/workout%20hip%20hop",
        badge = "Памп",
        letter = "G",
        accentHex = 0xFFA78BFA
    ),
    WorkoutPlaylist(
        id = "sp_synth_focus",
        title = "Synthwave Focus",
        subtitle = "Рівний темп для концентрації",
        emoji = "S",
        queryOrUrl = "https://open.spotify.com/search/workout%20electronic%20focus",
        badge = "Фокус",
        letter = "S",
        accentHex = 0xFF60A5FA
    )
)

val DEFAULT_WORKOUT_PLAYLISTS = YOUTUBE_MUSIC_PLAYLISTS

private fun loadPersistedPlaylists(context: Context, isYouTube: Boolean): List<WorkoutPlaylist> {
    val prefs = context.getSharedPreferences("apex_music_playlists_prefs", Context.MODE_PRIVATE)
    val key = if (isYouTube) "yt_playlists_json" else "sp_playlists_json"
    val raw = prefs.getString(key, null)
    val defaults = if (isYouTube) YOUTUBE_MUSIC_PLAYLISTS else SPOTIFY_PLAYLISTS
    if (raw.isNullOrBlank()) return defaults
    return try {
        val arr = JSONArray(raw)
        val result = mutableListOf<WorkoutPlaylist>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val title = obj.optString("title", "Плейлист")
            result.add(
                WorkoutPlaylist(
                    id = obj.optString("id", "custom_$i"),
                    title = title,
                    subtitle = obj.optString("subtitle", "Збережене посилання"),
                    emoji = obj.optString("letter", title.firstOrNull()?.uppercase() ?: "M"),
                    queryOrUrl = obj.optString("queryOrUrl", ""),
                    badge = obj.optString("badge", "Мій"),
                    letter = obj.optString("letter", title.firstOrNull()?.uppercase() ?: "M"),
                    accentHex = obj.optLong("accentHex", 0xFF2EE5B5)
                )
            )
        }
        if (result.isEmpty()) defaults else result
    } catch (e: Exception) {
        defaults
    }
}

private fun savePersistedPlaylists(context: Context, isYouTube: Boolean, list: List<WorkoutPlaylist>) {
    val prefs = context.getSharedPreferences("apex_music_playlists_prefs", Context.MODE_PRIVATE)
    val key = if (isYouTube) "yt_playlists_json" else "sp_playlists_json"
    val arr = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("subtitle", item.subtitle)
            put("queryOrUrl", item.queryOrUrl)
            put("badge", item.badge)
            put("letter", item.letter)
            put("accentHex", item.accentHex)
        }
        arr.put(obj)
    }
    prefs.edit().putString(key, arr.toString()).apply()
}

fun launchYouTubeMusic(context: Context, urlOrQuery: String? = null) {
    try {
        val targetUri = if (!urlOrQuery.isNullOrBlank()) {
            if (urlOrQuery.startsWith("http://") || urlOrQuery.startsWith("https://")) {
                Uri.parse(urlOrQuery)
            } else {
                Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(urlOrQuery))
            }
        } else {
            Uri.parse("https://music.youtube.com")
        }

        val ytMusicIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            setPackage("com.google.android.apps.youtube.music")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val packageManager = context.packageManager
        if (ytMusicIntent.resolveActivity(packageManager) != null) {
            context.startActivity(ytMusicIntent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Не вдалося відкрити YouTube Music: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun launchSpotify(context: Context, urlOrQuery: String? = null) {
    try {
        val targetUri = if (!urlOrQuery.isNullOrBlank()) {
            if (urlOrQuery.startsWith("spotify:") || urlOrQuery.startsWith("http://") || urlOrQuery.startsWith("https://")) {
                Uri.parse(urlOrQuery)
            } else {
                Uri.parse("https://open.spotify.com/search/" + Uri.encode(urlOrQuery))
            }
        } else {
            Uri.parse("spotify:")
        }

        val spotifyIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            setPackage("com.spotify.music")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val packageManager = context.packageManager
        if (spotifyIntent.resolveActivity(packageManager) != null) {
            context.startActivity(spotifyIntent)
        } else {
            val fallbackUri = if (targetUri.scheme == "spotify") {
                val str = targetUri.toString()
                if (str.startsWith("spotify:search:")) {
                    Uri.parse("https://open.spotify.com/search/" + Uri.encode(str.removePrefix("spotify:search:")))
                } else if (str.startsWith("spotify:playlist:")) {
                    Uri.parse("https://open.spotify.com/playlist/" + str.removePrefix("spotify:playlist:"))
                } else {
                    Uri.parse("https://open.spotify.com")
                }
            } else {
                targetUri
            }
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Не вдалося відкрити Spotify: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WorkoutMusicLauncherDialog(
    preferredPlatform: String = "YOUTUBE_MUSIC",
    onSelectPlatform: (String) -> Unit = {},
    customYtPlaylistUrl: String = "",
    onSaveCustomYtPlaylistUrl: (String) -> Unit = {},
    customSpotifyPlaylistUrl: String = "",
    onSaveCustomSpotifyPlaylistUrl: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedService by remember(preferredPlatform) {
        mutableStateOf(
            if (preferredPlatform.equals("SPOTIFY", ignoreCase = true)) {
                MusicServiceType.SPOTIFY
            } else {
                MusicServiceType.YOUTUBE_MUSIC
            }
        )
    }

    val isYouTube = selectedService == MusicServiceType.YOUTUBE_MUSIC

    var ytPlaylists by remember { mutableStateOf(loadPersistedPlaylists(context, true)) }
    var spPlaylists by remember { mutableStateOf(loadPersistedPlaylists(context, false)) }

    val activePlaylists = if (isYouTube) ytPlaylists else spPlaylists

    var inputCustomUrl by remember(isYouTube) { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<WorkoutPlaylist?>(null) }
    var showAddPlaylistModal by remember { mutableStateOf(false) }

    fun updateActivePlaylists(newList: List<WorkoutPlaylist>) {
        if (isYouTube) {
            ytPlaylists = newList
            savePersistedPlaylists(context, true, newList)
        } else {
            spPlaylists = newList
            savePersistedPlaylists(context, false, newList)
        }
    }

    fun handleQuickAddUrl() {
        val cleanUrl = inputCustomUrl.trim()
        if (cleanUrl.isEmpty()) return
        if (isYouTube) {
            onSaveCustomYtPlaylistUrl(cleanUrl)
        } else {
            onSaveCustomSpotifyPlaylistUrl(cleanUrl)
        }
        val currentList = activePlaylists.toMutableList()
        val myMixIdx = currentList.indexOfFirst { it.id.endsWith("my_mix") || it.badge == "Мій" }
        if (myMixIdx >= 0 && currentList[myMixIdx].title == "Мій мікс") {
            currentList[myMixIdx] = currentList[myMixIdx].copy(
                queryOrUrl = cleanUrl,
                subtitle = cleanUrl
            )
        } else {
            currentList.add(
                0,
                WorkoutPlaylist(
                    id = "custom_${System.currentTimeMillis()}",
                    title = "Мій плейлист ${currentList.count { it.badge == "Мій" } + 1}",
                    subtitle = cleanUrl,
                    emoji = "M",
                    queryOrUrl = cleanUrl,
                    badge = "Мій",
                    letter = "M",
                    accentHex = 0xFF2EE5B5
                )
            )
        }
        updateActivePlaylists(currentList)
        inputCustomUrl = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workout_music_dialog")
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)
                        .imePadding()
                ) {
                    // Top Drag Handle
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF33434B))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Музика",
                                fontFamily = Unbounded,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Відкриється в застосунку сервісу",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onDismiss() }
                                .testTag("close_music_dialog_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрити",
                                    tint = SteelBlueOnContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Service Segmented Switcher (YouTube Music | Spotify)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_platform_tabs")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // YouTube Music Tab
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isYouTube) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedService = MusicServiceType.YOUTUBE_MUSIC
                                        onSelectPlatform(MusicServiceType.YOUTUBE_MUSIC.id)
                                    }
                                    .testTag("tab_youtube_music")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF0033))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "YouTube Music",
                                        fontSize = 14.sp,
                                        fontWeight = if (isYouTube) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isYouTube) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Spotify Tab
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isYouTube) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedService = MusicServiceType.SPOTIFY
                                        onSelectPlatform(MusicServiceType.SPOTIFY.id)
                                    }
                                    .testTag("tab_spotify")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1DB954))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Spotify",
                                        fontSize = 14.sp,
                                        fontWeight = if (!isYouTube) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (!isYouTube) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // МІЙ ПЛЕЙЛИСТ Section
                    Text(
                        text = "МІЙ ПЛЕЙЛИСТ",
                        style = ApexSectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_playlist_card"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputCustomUrl,
                            onValueChange = { inputCustomUrl = it },
                            placeholder = {
                                Text(
                                    text = "Вставте посилання на плейлист",
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    color = TextMutedDark
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedBorderColor = IceCyanPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { handleQuickAddUrl() }),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("custom_playlist_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SteelBlueContainer,
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { handleQuickAddUrl() }
                                .testTag("save_custom_playlist_btn")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ДОДАТИ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IceCyanPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ПЛЕЙЛИСТИ (N) Header + Змінити Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ПЛЕЙЛИСТИ (${activePlaylists.size})",
                            style = ApexSectionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            onClick = { isEditMode = !isEditMode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isEditMode) IceCyanPrimary else Color.Transparent,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("edit_playlists_mode_btn")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEditMode) "Готово" else "Змінити",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isEditMode) IceCyanOnPrimary else IceCyanPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = IceCyanContainer,
                            border = BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddPlaylistModal = true }
                                .testTag("add_custom_playlist_dialog_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = IceCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Додати власний плейлист",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IceCyanPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Playlists List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    ) {
                        if (activePlaylists.isEmpty()) {
                            item(key = "empty_playlists") {
                                Text(
                                    text = "Плейлистів немає. Додайте свій за посиланням вище.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp)
                                )
                            }
                        }
                        itemsIndexed(activePlaylists, key = { _, pl -> pl.id }) { index, playlist ->
                            val accentColor = Color(playlist.accentHex)

                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isEditMode) {
                                            editingPlaylist = playlist
                                        } else {
                                            if (isYouTube) {
                                                launchYouTubeMusic(context, playlist.queryOrUrl)
                                            } else {
                                                launchSpotify(context, playlist.queryOrUrl)
                                            }
                                            onDismiss()
                                        }
                                    }
                                    .testTag("playlist_item_${playlist.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 68.dp)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Colored Letter Avatar
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = accentColor.copy(alpha = 0.16f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = playlist.letter.take(1).uppercase(),
                                                    fontFamily = Unbounded,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = playlist.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = accentColor.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = playlist.badge,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = accentColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = playlist.subtitle,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isEditMode) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { editingPlaylist = playlist },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("edit_playlist_btn_${playlist.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Редагувати плейлист",
                                                    tint = IceCyanPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    updateActivePlaylists(activePlaylists.filter { it.id != playlist.id })
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("delete_playlist_btn_${playlist.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Видалити плейлист",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Увімкнути плейлист ${playlist.title}",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Primary CTA Button
                    Button(
                        onClick = {
                            if (isYouTube) {
                                launchYouTubeMusic(context, customYtPlaylistUrl.ifBlank { null })
                            } else {
                                launchSpotify(context, customSpotifyPlaylistUrl.ifBlank { null })
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyanPrimary,
                            contentColor = IceCyanOnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag(if (isYouTube) "open_yt_music_app_btn" else "open_spotify_app_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = IceCyanOnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isYouTube) "Відкрити YouTube Music" else "Відкрити Spotify",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = IceCyanOnPrimary
                        )
                    }
                }
            }
        }
    }

    // Edit Playlist Dialog
    editingPlaylist?.let { item ->
        var editTitle by remember(item) { mutableStateOf(item.title) }
        var editSubtitle by remember(item) { mutableStateOf(item.subtitle) }
        var editUrl by remember(item) { mutableStateOf(item.queryOrUrl) }
        var editBadge by remember(item) { mutableStateOf(item.badge) }

        AlertDialog(
            onDismissRequest = { editingPlaylist = null },
            title = { Text("Редагувати плейлист", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Назва плейлиста") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_playlist_title_input")
                    )
                    OutlinedTextField(
                        value = editBadge,
                        onValueChange = { editBadge = it },
                        label = { Text("Категорія (бейдж)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = { editSubtitle = it },
                        label = { Text("Опис") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("Посилання або пошуковий запит") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = editTitle.trim().ifEmpty { item.title }
                        val updated = item.copy(
                            title = cleanTitle,
                            subtitle = editSubtitle.trim().ifEmpty { "Власний плейлист" },
                            queryOrUrl = editUrl.trim(),
                            badge = editBadge.trim().ifEmpty { "Мій" },
                            letter = cleanTitle.firstOrNull()?.uppercase() ?: "M"
                        )
                        updateActivePlaylists(
                            activePlaylists.map { if (it.id == item.id) updated else it }
                        )
                        editingPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.testTag("confirm_edit_playlist_btn")
                ) {
                    Text("Зберегти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlaylist = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Add Custom Playlist Dialog
    if (showAddPlaylistModal) {
        var newTitle by remember { mutableStateOf("") }
        var newSubtitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }
        var newBadge by remember { mutableStateOf("Мій") }

        AlertDialog(
            onDismissRequest = { showAddPlaylistModal = false },
            title = { Text("Новий плейлист", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Назва плейлиста") },
                        placeholder = { Text("Напр. Важкий жим") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_playlist_title_input")
                    )
                    OutlinedTextField(
                        value = newBadge,
                        onValueChange = { newBadge = it },
                        label = { Text("Категорія") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubtitle,
                        onValueChange = { newSubtitle = it },
                        label = { Text("Опис") },
                        placeholder = { Text("Власна добірка") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("Посилання або запит") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_playlist_url_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = newTitle.trim().ifEmpty { "Власний плейлист" }
                        val created = WorkoutPlaylist(
                            id = "custom_${System.currentTimeMillis()}",
                            title = cleanTitle,
                            subtitle = newSubtitle.trim().ifEmpty { "Збережене посилання" },
                            emoji = cleanTitle.firstOrNull()?.uppercase() ?: "M",
                            queryOrUrl = newUrl.trim(),
                            badge = newBadge.trim().ifEmpty { "Мій" },
                            letter = cleanTitle.firstOrNull()?.uppercase() ?: "M",
                            accentHex = 0xFF2EE5B5
                        )
                        updateActivePlaylists(listOf(created) + activePlaylists)
                        showAddPlaylistModal = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.testTag("confirm_new_playlist_btn")
                ) {
                    Text("Додати", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistModal = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun YouTubeMusicLauncherDialog(
    customPlaylistUrl: String,
    onSaveCustomPlaylistUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    WorkoutMusicLauncherDialog(
        preferredPlatform = "YOUTUBE_MUSIC",
        customYtPlaylistUrl = customPlaylistUrl,
        onSaveCustomYtPlaylistUrl = onSaveCustomPlaylistUrl,
        onDismiss = onDismiss
    )
}
