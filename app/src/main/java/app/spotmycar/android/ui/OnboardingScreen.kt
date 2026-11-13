package app.spotmycar.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.ui.theme.Outfit
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking

private data class IntroPage(val title: Int, val body: Int)

private val PAGES = listOf(
    IntroPage(R.string.intro_1_title, R.string.intro_1_body),
    IntroPage(R.string.intro_2_title, R.string.intro_2_body),
    IntroPage(R.string.intro_3_title, R.string.intro_3_body),
)

/** Trois pages pour comprendre l'app, montrées une seule fois. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = page == PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 28.dp),
    ) {
        Wordmark(size = 19.sp)

        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "intro",
        ) { index ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(SmcBigCardShape)
                        .background(Smc.AccentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    when (index) {
                        0 -> IntroBluetooth()
                        1 -> IntroGarage()
                        else -> IntroGuide()
                    }
                }
                Spacer(Modifier.height(26.dp))
                CondensedTitle(text = stringResource(PAGES[index].title), size = 44.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(PAGES[index].body),
                    color = Smc.Muted,
                    fontFamily = Outfit,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PAGES.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .animateContentSize()
                            .width(if (index == page) 22.dp else 6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (index == page) Smc.Accent else Smc.Off),
                    )
                }
            }
            Button(
                onClick = { if (last) onFinished() else page += 1 },
                modifier = Modifier.heightIn(min = 56.dp),
                shape = SmcShape,
                colors = ButtonDefaults.buttonColors(containerColor = Smc.Accent, contentColor = Smc.OnAccent),
            ) {
                Text(
                    text = stringResource(if (last) R.string.intro_start else R.string.intro_next),
                    fontFamily = Outfit,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

/** Page 1 : le Bluetooth se coupe, une onde part. */
@Composable
private fun IntroBluetooth() {
    PinRings(inward = false, modifier = Modifier.size(200.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Smc.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bluetooth),
                contentDescription = null,
                tint = Smc.Accent,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

/** Page 2 : niveau, zone, place. */
@Composable
private fun IntroGarage() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        GarageTile(label = stringResource(R.string.level_label), value = "-2", highlighted = true)
        GarageTile(label = stringResource(R.string.zone_label), value = "C", highlighted = false)
        GarageTile(label = stringResource(R.string.place_label), value = "148", highlighted = false)
    }
}

/** Page 3 : on vous ramène, les ondes reviennent vers la flèche. */
@Composable
private fun IntroGuide() {
    PinRings(inward = true, modifier = Modifier.size(200.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_navigation),
            contentDescription = null,
            tint = Smc.Accent,
            modifier = Modifier.size(64.dp),
        )
    }
}

@Composable
private fun GarageTile(label: String, value: String, highlighted: Boolean) {
    Column(
        modifier = Modifier
            .clip(SmcShape)
            .background(if (highlighted) Smc.Accent else Smc.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = if (highlighted) Smc.OnAccent else Smc.Dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            color = if (highlighted) Smc.OnAccent else Smc.Text,
            fontFamily = Outfit,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
        )
    }
}
