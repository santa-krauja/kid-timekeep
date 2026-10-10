package lv.zarin.timekeep.ui.hourglass

import androidx.annotation.DrawableRes
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.timer.PictureId

@DrawableRes
fun PictureId.drawableRes(): Int = when (this) {
    PictureId.HEART -> R.drawable.emoji_heart
    PictureId.STAR -> R.drawable.emoji_star
    PictureId.BLOSSOM -> R.drawable.emoji_blossom
    PictureId.SMILEY -> R.drawable.emoji_smiley
    PictureId.SUN -> R.drawable.emoji_sun
    PictureId.MOON -> R.drawable.emoji_moon
    PictureId.FISH -> R.drawable.emoji_fish
    PictureId.CAT -> R.drawable.emoji_cat
    PictureId.DOG -> R.drawable.emoji_dog
    PictureId.CAR -> R.drawable.emoji_car
    PictureId.TREE -> R.drawable.emoji_tree
    PictureId.BUTTERFLY -> R.drawable.emoji_butterfly
    PictureId.APPLE -> R.drawable.emoji_apple
    PictureId.RAINBOW -> R.drawable.emoji_rainbow
    PictureId.UNICORN -> R.drawable.emoji_unicorn
    PictureId.ROCKET -> R.drawable.emoji_rocket
}
