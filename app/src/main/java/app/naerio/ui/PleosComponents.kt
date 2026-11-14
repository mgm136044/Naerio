package app.naerio.ui

// insane-design: pleos + ui-ux-pro-max + kill-ai-slop (2026-11-15).
// 원칙: 글보다 아이콘과 도식. 상태 색은 아이콘에만 쓰고 배경 틴트 칩은 쓰지 않는다. 아이콘은 Material Rounded 한 계열.

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(24.dp)
val ControlShape = RoundedCornerShape(20.dp)
const val TouchMin = 76 // dp, Design for Driving 최소 터치 크기

/** 아이콘 크기 토큰 (ui-ux-pro-max: Consistent Icon Sizing) */
object IconSize { val S = 28.dp; val M = 36.dp; val L = 48.dp; val XL = 64.dp }

/** Pleos Connect Interaction: 누르는 동안 95%. 레이아웃 경계는 바꾸지 않는다. */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, tween(120), label = "press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

enum class BtnKind { FILLED, BASIC, GHOST }

@Composable
fun PleosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: BtnKind = BtnKind.FILLED,
    height: Dp = 88.dp,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val c = Ng.c
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg = when {
        kind == BtnKind.GHOST -> if (pressed) c.sunken else Color.Transparent
        !enabled -> c.sunken
        kind == BtnKind.FILLED -> if (pressed) c.ctaPressed else c.cta
        else -> if (pressed) c.basicBtnPressed else c.basicBtn
    }
    val fg = when {
        !enabled -> c.muted
        kind == BtnKind.FILLED -> c.onCta
        kind == BtnKind.GHOST -> c.ink2
        else -> c.ink
    }
    Row(
        modifier
            .pressScale(src)
            .heightIn(min = height)
            .clip(ControlShape)
            .background(bg)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (kind == BtnKind.GHOST) 16.dp else 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(IconSize.S)); Spacer(Modifier.width(12.dp)) }
        Text(text, style = if (height < 80.dp) Ng.t.bodyM.copy(fontWeight = FontWeight.W600) else Ng.t.button, color = fg, maxLines = 1)
        trailingIcon?.let { Spacer(Modifier.width(12.dp)); Icon(it, null, tint = fg, modifier = Modifier.size(IconSize.S)) }
    }
}

/** 아이콘만 있는 버튼. 터치 영역은 76dp를 유지하고 설명(contentDescription)을 반드시 단다. */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Ng.c.ink3, selected: Boolean = false) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(TouchMin.dp).pressScale(src).clip(CircleShape)
            .background(if (selected) Ng.c.sunken else Color.Transparent)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(IconSize.M)) }
}

data class SegOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** 회색 컨테이너 위 흰 pill (Pleos Segmented). 안쪽 반경 = 바깥 20 - 여백 6 = 14. */
@Composable
fun <T> PleosSegmented(
    options: List<SegOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    itemHeight: Dp = TouchMin.dp,
) {
    val c = Ng.c
    val inner = RoundedCornerShape(14.dp)
    Row(modifier.clip(ControlShape).background(c.sunken).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            val on = o.value == selected
            val src = remember { MutableInteractionSource() }
            Row(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .pressScale(src)
                    .heightIn(min = itemHeight)
                    .then(if (on) Modifier.shadow(1.dp, inner, clip = false) else Modifier)
                    .clip(inner)
                    .background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, interactionSource = src, indication = null, role = Role.Tab) { onSelect(o.value) }
                    .padding(horizontal = if (fill) 12.dp else 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                o.icon?.let { Icon(it, null, tint = if (on) c.ink else c.ink3, modifier = Modifier.size(IconSize.S)); Spacer(Modifier.width(10.dp)) }
                Text(
                    o.label, style = if (on) Ng.t.bodyM.copy(fontWeight = FontWeight.W600) else Ng.t.bodyM,
                    color = if (on) c.ink else c.ink3, maxLines = 1, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 한 영역에 표면 하나. 선택되면 파랑 3dp 테두리와 체크(테두리와 반경은 같은 상자에 둔다). */
@Composable
fun NgCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    padding: Dp = 32.dp,
    gap: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ng.c
    val src = remember { MutableInteractionSource() }
    val base = modifier
        .then(if (onClick != null && enabled) Modifier.pressScale(src) else Modifier)
        .clip(CardShape)
        .background(c.surface)
        .then(
            when {
                selected -> Modifier.border(3.dp, c.accent, CardShape)
                c.isDark -> Modifier.border(1.dp, c.line, CardShape)
                else -> Modifier
            }
        )
        .then(
            if (onClick != null) Modifier.selectable(
                selected = selected, interactionSource = src, indication = null, enabled = enabled,
                role = Role.RadioButton, onClick = onClick,
            ) else Modifier
        )
    Box(base) {
        Column(
            Modifier.fillMaxWidth().padding(padding).padding(end = if (onClick != null) 44.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(gap), content = content,
        )
        if (selected) Icon(
            Icons.Rounded.CheckCircle, contentDescription = "선택됨", tint = c.accent,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(IconSize.L),
        )
    }
}

enum class Tone { OK, UNKNOWN, NO, NEUTRAL }

/** 상태는 아이콘 모양 + 단어로 전달한다. 색은 '안 됨'에만 쓴다(kill-ai-slop #04, #05). */
@Composable
fun StatusIcon(tone: Tone, size: Dp = IconSize.M) {
    val c = Ng.c
    val (icon, tint) = when (tone) {
        Tone.OK -> Icons.Rounded.CheckCircle to c.ink
        Tone.UNKNOWN -> Icons.AutoMirrored.Rounded.Help to c.ink3
        Tone.NO -> Icons.Rounded.Cancel to c.warn
        Tone.NEUTRAL -> Icons.Rounded.RemoveCircle to c.muted
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun StatusLine(tone: Tone, text: String, size: Dp = IconSize.M) = Row(verticalAlignment = Alignment.CenterVertically) {
    StatusIcon(tone, size)
    Spacer(Modifier.width(12.dp))
    Text(text, style = Ng.t.bodyM.copy(fontWeight = FontWeight.W600), color = Ng.c.ink)
}

/** 사실 하나를 아이콘으로: 큰 아이콘 + 모서리 상태 표식 + 한 단어 캡션. */
@Composable
fun FactIcon(icon: ImageVector, tone: Tone, caption: String, description: String, big: Boolean = false) {
    val c = Ng.c
    Column(
        Modifier.width(if (big) 160.dp else 120.dp).semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(if (big) 104.dp else 64.dp)) {
            Icon(icon, null, tint = if (tone == Tone.NEUTRAL) c.muted else c.ink2, modifier = Modifier.align(Alignment.Center).size(if (big) 76.dp else IconSize.L))
            Box(Modifier.align(Alignment.BottomEnd).size(if (big) 44.dp else 30.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                StatusIcon(tone, if (big) 38.dp else 26.dp)
            }
        }
        Text(caption, style = Ng.t.label, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun IconText(icon: ImageVector, text: String, color: Color = Ng.c.ink3, iconSize: Dp = IconSize.S) =
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(8.dp))
        Text(text, style = Ng.t.label, color = color, maxLines = 1)
    }

enum class BannerKind { INFO, WARN }

/** 알림은 중립 면 위에. 경고는 아이콘만 빨강, 글자는 본문색(kill-ai-slop #05). */
@Composable
fun Banner(text: String, kind: BannerKind = BannerKind.INFO, modifier: Modifier = Modifier) {
    val c = Ng.c
    val warn = kind == BannerKind.WARN
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.sunken).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (warn) Icons.Rounded.Warning else Icons.Rounded.Info, null, tint = if (warn) c.warn else c.ink3, modifier = Modifier.size(IconSize.S))
        Spacer(Modifier.width(14.dp))
        Text(text, style = Ng.t.bodyS, color = if (warn) c.ink else c.ink2)
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Ng.c.line))

// ---------------- 경로 도식 (signature: route-strip-diagram) ----------------

enum class NodeKind { DROPOFF, PARKING, DESTINATION }

data class RouteNode(val kind: NodeKind, val mark: String, val caption: String)

@Composable
fun PlaceNode(kind: NodeKind, mark: String, size: Dp = 56.dp) {
    val c = Ng.c
    when (kind) {
        NodeKind.DROPOFF -> Box(Modifier.size(size).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
            if (mark.isEmpty()) Icon(Icons.Rounded.Hail, null, tint = c.surface, modifier = Modifier.size(size * 0.6f))
            else Text(mark, style = Ng.t.titleS, color = c.surface)
        }
        NodeKind.PARKING -> Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(c.parking), contentAlignment = Alignment.Center) {
            Text(mark.ifEmpty { "P" }, style = Ng.t.titleS, color = Color.White)
        }
        NodeKind.DESTINATION -> Box(
            Modifier.size(size).clip(CircleShape).border(3.dp, c.ink, CircleShape).background(c.surface),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Apartment, null, tint = c.ink, modifier = Modifier.size(size * 0.55f)) }
    }
}

@Composable
private fun DriveSegment(modifier: Modifier) {
    val col = Ng.c.ink2
    Canvas(modifier.height(8.dp)) {
        drawLine(col, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun WalkSegment(modifier: Modifier) {
    val col = Ng.c.accent
    Canvas(modifier.height(8.dp)) {
        drawLine(
            col, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 14.dp.toPx()), 0f),
        )
    }
}

/** 가로 경로 도식: 차로 가는 구간은 실선, 걸어가는 구간은 점선. */
@Composable
fun RouteStrip(nodes: List<RouteNode>, modifier: Modifier = Modifier, segment: Dp = 96.dp, node: Dp = 56.dp, captions: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        nodes.forEachIndexed { i, n ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = node + 8.dp)) {
                PlaceNode(n.kind, n.mark, node)
                if (captions) {
                    Spacer(Modifier.height(8.dp))
                    Text(n.caption, style = Ng.t.label, color = Ng.c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (i < nodes.lastIndex) {
                val seg = Modifier.padding(top = node / 2 - 4.dp).width(segment)
                if (nodes[i + 1].kind == NodeKind.DESTINATION) WalkSegment(seg) else DriveSegment(seg)
            }
        }
    }
}

/** 단계 표시: 순서가 실제로 있는 흐름이라 4칸 막대로 보여 준다(장식용 01/02 숫자 대신). */
@Composable
fun StepBars(current: Int, labels: List<String>) {
    val c = Ng.c
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(labels.getOrElse(current - 1) { "" }, style = Ng.t.label.copy(fontWeight = FontWeight.W600), color = c.ink2)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = "${labels.size}단계 중 ${current}단계" }) {
            labels.indices.forEach { i ->
                val n = i + 1
                Box(
                    Modifier.width(44.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(when { n == current -> c.ink; n < current -> c.ink3; else -> c.line })
                )
            }
        }
    }
}

/** 확인 항목 라벨 → 아이콘 + 한 단어 캡션 */
fun checkVisual(label: String): Pair<ImageVector, String> = when {
    "계단" in label -> Icons.Rounded.Accessible to "계단 없음"
    "지붕" in label -> Icons.Rounded.Umbrella to "지붕"
    "하차 후 주차장" in label -> Icons.Rounded.DirectionsCar to "주차장 이동"
    "주차 후 보행" in label -> Icons.AutoMirrored.Rounded.DirectionsWalk to "주차 후 도보"
    "보행" in label -> Icons.AutoMirrored.Rounded.DirectionsWalk to "도보 연결"
    "방문객 주차" in label -> Icons.Rounded.LocalParking to "방문객 주차"
    "하차" in label -> Icons.Rounded.Hail to "하차"
    "차량" in label -> Icons.Rounded.DirectionsCar to "차량 접근"
    else -> Icons.Rounded.Info to label
}
