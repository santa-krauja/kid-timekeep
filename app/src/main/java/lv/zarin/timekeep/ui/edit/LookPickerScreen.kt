package lv.zarin.timekeep.ui.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.ui.hourglass.Hourglass
import lv.zarin.timekeep.ui.hourglass.drawableRes

/** Static fill used for every preview glass (wireframe E1/E2). */
internal const val PREVIEW_PROGRESS = 0.35f

/**
 * Wireframe E2: preview, favourite looks, top and bottom pictures and sands, Done.
 * In-screen state of [EditTimerScreen], not a navigation route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookPickerScreen(
    look: Look,
    favourites: List<FavouriteLook>,
    onLookChange: (Look) -> Unit,
    onSaveFavourite: () -> Unit,
    onDeleteFavourite: (String) -> Unit,
    onDone: () -> Unit,
    canDelete: Boolean = true,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.look_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.action_done))
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Hourglass(
                    look = look,
                    progress = { PREVIEW_PROGRESS },
                    running = false,
                    modifier = Modifier.height(120.dp).aspectRatio(0.62f),
                    contentDescription = lookDescription(look),
                )
            }

            FieldLabel(stringResource(R.string.look_favourites))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (fav in favourites) {
                    FavouriteThumb(
                        fav = fav,
                        selected = fav.look == look,
                        onUse = { onLookChange(fav.look) },
                        onDelete = { onDeleteFavourite(fav.id) }.takeIf { canDelete },
                    )
                }
                OutlinedButton(onClick = onSaveFavourite, enabled = favourites.none { it.look == look }) {
                    Icon(Icons.Rounded.StarOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_save_look), modifier = Modifier.padding(start = 4.dp))
                }
            }

            FieldLabel(stringResource(R.string.look_top_picture))
            PictureRow(look.top, look.topSand) { onLookChange(look.copy(top = it)) }
            FieldLabel(stringResource(R.string.look_top_sand))
            SandRow(look.topSand) { onLookChange(look.copy(topSand = it)) }
            FieldLabel(stringResource(R.string.look_bottom_picture))
            PictureRow(look.bottom, look.bottomSand) { onLookChange(look.copy(bottom = it)) }
            FieldLabel(stringResource(R.string.look_bottom_sand))
            SandRow(look.bottomSand) { onLookChange(look.copy(bottomSand = it)) }
        }
    }
}

@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavouriteThumb(fav: FavouriteLook, selected: Boolean, onUse: () -> Unit, onDelete: (() -> Unit)?) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(2.dp, border), RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onUse,
                onLongClick = onDelete,
                onLongClickLabel = onDelete?.let { stringResource(R.string.action_delete) },
            )
            .padding(6.dp),
    ) {
        Hourglass(
            look = fav.look,
            progress = { PREVIEW_PROGRESS },
            running = false,
            modifier = Modifier.height(56.dp).aspectRatio(0.62f),
            contentDescription = lookDescription(fav.look),
        )
    }
}

/** Pictures that clash with [sand] are dimmed (still selectable). */
@Composable
private fun PictureRow(selected: PictureId, sand: SandColor, onPick: (PictureId) -> Unit) {
    // Start scrolled so the chosen picture is visible.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selected.ordinal - 2).coerceAtLeast(0))
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PictureId.entries) { pic ->
            val isOn = pic == selected
            val name = stringResource(pic.labelRes())
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(sand.argb))
                    .border(
                        BorderStroke(if (isOn) 3.dp else 0.dp, if (isOn) MaterialTheme.colorScheme.primary else Color.Transparent),
                        CircleShape,
                    )
                    .selectable(selected = isOn, role = Role.RadioButton, onClick = { onPick(pic) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(pic.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).alpha(if (LookPicker.contrastOk(pic, sand)) 1f else 0.35f),
                )
            }
        }
    }
}

@Composable
private fun SandRow(selected: SandColor, onPick: (SandColor) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (sand in SandColor.entries) {
            val isOn = sand == selected
            val name = stringResource(sand.labelRes())
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = isOn, role = Role.RadioButton, onClick = { onPick(sand) })
                    .semantics { contentDescription = name }
                    .border(
                        BorderStroke(3.dp, if (isOn) MaterialTheme.colorScheme.primary else Color.Transparent),
                        CircleShape,
                    )
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color(sand.argb))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
            )
        }
    }
}

/** "Heart on Lavender sand, Star on Sky sand". */
@Composable
internal fun lookDescription(look: Look): String = stringResource(
    R.string.cd_look,
    stringResource(look.top.labelRes()),
    stringResource(look.topSand.labelRes()),
    stringResource(look.bottom.labelRes()),
    stringResource(look.bottomSand.labelRes()),
)

@StringRes
internal fun PictureId.labelRes(): Int = when (this) {
    PictureId.HEART -> R.string.picture_heart
    PictureId.STAR -> R.string.picture_star
    PictureId.BLOSSOM -> R.string.picture_blossom
    PictureId.SMILEY -> R.string.picture_smiley
    PictureId.SUN -> R.string.picture_sun
    PictureId.MOON -> R.string.picture_moon
    PictureId.FISH -> R.string.picture_fish
    PictureId.CAT -> R.string.picture_cat
    PictureId.DOG -> R.string.picture_dog
    PictureId.CAR -> R.string.picture_car
    PictureId.TREE -> R.string.picture_tree
    PictureId.BUTTERFLY -> R.string.picture_butterfly
    PictureId.APPLE -> R.string.picture_apple
    PictureId.RAINBOW -> R.string.picture_rainbow
    PictureId.UNICORN -> R.string.picture_unicorn
    PictureId.ROCKET -> R.string.picture_rocket
}

@StringRes
internal fun SandColor.labelRes(): Int = when (this) {
    SandColor.LAVENDER -> R.string.sand_lavender
    SandColor.SKY -> R.string.sand_sky
    SandColor.MINT -> R.string.sand_mint
    SandColor.PEACH -> R.string.sand_peach
    SandColor.SAND -> R.string.sand_sand
    SandColor.PINK -> R.string.sand_pink
    SandColor.NIGHT -> R.string.sand_night
    SandColor.LEMON -> R.string.sand_lemon
}
