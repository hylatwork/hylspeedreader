package eu.hylatwork.hylspeedreader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of transport glyphs the reader needs, authored directly as
 * [ImageVector]s. This replaces `androidx.compose.material:material-icons-extended`
 * (~35 MB of generated icons in the build) with ~6 simple polygons; only
 * `material-icons-core` (already shipped with `material3`) stays for
 * `Icons.Filled.Menu` and `Icons.Filled.PlayArrow`.
 *
 * The vector fill colour is irrelevant: `Icon` tints the whole path via
 * `LocalContentColor`/its `tint` parameter.
 */
object HylIcons {

    val Pause: ImageVector = icon("Pause") {
        moveTo(6f, 5f); lineTo(10f, 5f); lineTo(10f, 19f); lineTo(6f, 19f); close()
        moveTo(14f, 5f); lineTo(18f, 5f); lineTo(18f, 19f); lineTo(14f, 19f); close()
    }

    val FastForward: ImageVector = icon("FastForward") {
        moveTo(3f, 6f); lineTo(11f, 12f); lineTo(3f, 18f); close()
        moveTo(13f, 6f); lineTo(21f, 12f); lineTo(13f, 18f); close()
    }

    val FastRewind: ImageVector = icon("FastRewind") {
        moveTo(21f, 6f); lineTo(13f, 12f); lineTo(21f, 18f); close()
        moveTo(11f, 6f); lineTo(3f, 12f); lineTo(11f, 18f); close()
    }

    val SkipNext: ImageVector = icon("SkipNext") {
        moveTo(6f, 6f); lineTo(16f, 12f); lineTo(6f, 18f); close()
        moveTo(16f, 6f); lineTo(19f, 6f); lineTo(19f, 18f); lineTo(16f, 18f); close()
    }

    val SkipPrevious: ImageVector = icon("SkipPrevious") {
        moveTo(18f, 6f); lineTo(8f, 12f); lineTo(18f, 18f); close()
        moveTo(5f, 6f); lineTo(8f, 6f); lineTo(8f, 18f); lineTo(5f, 18f); close()
    }

    /** Open book, used for the "Open EPUB" action and the empty state. */
    val MenuBook: ImageVector = icon("MenuBook") {
        moveTo(3f, 5f); lineTo(11f, 6.5f); lineTo(11f, 19f); lineTo(3f, 17.5f); close()
        moveTo(13f, 6.5f); lineTo(21f, 5f); lineTo(21f, 17.5f); lineTo(13f, 19f); close()
    }

    /** Solid play triangle (the transport's primary action). */
    val PlayArrow: ImageVector = icon("PlayArrow") {
        moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
    }

    /** Hamburger menu, used to open the chapter drawer. */
    val Menu: ImageVector = icon("Menu") {
        moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 8f); lineTo(3f, 8f); close()
        moveTo(3f, 11f); lineTo(21f, 11f); lineTo(21f, 13f); lineTo(3f, 13f); close()
        moveTo(3f, 16f); lineTo(21f, 16f); lineTo(21f, 18f); lineTo(3f, 18f); close()
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) { block() }
        }.build()
}
