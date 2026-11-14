package app.naerio.ui

// Naerio(내리오) 화면. insane-design: pleos + ui-ux-pro-max + kill-ai-slop (2026-11-15).
// 본 화면은 아이콘과 경로 도식 위주로 짧게, 설명은 첫 실행 온보딩과 정보(i) 버튼으로 옮겼다.

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.naerio.BuildConfig
import app.naerio.R
import app.naerio.app.*
import app.naerio.data.LoadResult
import app.naerio.domain.*
import app.naerio.navigation.*
import app.naerio.vehicle.Interaction

private val HPad = 40.dp
private val TwoPaneMin = 1200.dp
private val StepLabels = listOf("목적지", "방문 방식", "후보", "확인")
private const val ONBOARDING_KEY = "onboarding_done_v1"

@Composable
fun NaerilgotApp(vm: AppViewModel) = NaerilgotTheme {
    val s by vm.ui.collectAsStateWithLifecycle()
    val c = Ng.c
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("onboarding", Context.MODE_PRIVATE) }
    var onboarding by rememberSaveable { mutableStateOf(!prefs.getBoolean(ONBOARDING_KEY, false)) }
    var notice by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(vm, s, onHelp = { onboarding = true }, onNotice = { notice = true })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                onboarding -> Onboarding(s.data?.takeIf { it.dataKind == DataKind.FICTIONAL_SAMPLE }?.notice) {
                    prefs.edit().putBoolean(ONBOARDING_KEY, true).apply(); onboarding = false
                }
                else -> when (val load = s.load) {
                    null -> Text("장소 데이터를 불러오는 중", Modifier.padding(HPad), style = Ng.t.bodyL, color = c.ink2)
                    is LoadResult.Invalid -> DataError(load.errors)
                    is LoadResult.Loaded -> when (s.screen) {
                        Screen.DESTINATION -> DestinationScreen(vm, s)
                        Screen.VISIT -> VisitScreen(vm, s)
                        Screen.CANDIDATES -> CandidatesScreen(vm, s)
                        Screen.CONFIRM -> ConfirmScreen(vm, s)
                        Screen.DIAGNOSTICS -> if (BuildConfig.DEBUG) DiagnosticsScreen(vm, s) else DestinationScreen(vm, s)
                    }
                }
            }
        }
    }
    if (notice) s.data?.notice?.let { NoticeDialog(it) { notice = false } }
}

// ---------------- 공통 ----------------

@Composable
private fun BrandMark() {
    val c = Ng.c
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Pleos.Basic900)
            .then(if (c.isDark) Modifier.border(1.dp, c.line, RoundedCornerShape(14.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(R.drawable.ic_naerio_mark), contentDescription = null, modifier = Modifier.size(40.dp)) }
}

@Composable
private fun TopBar(vm: AppViewModel, s: UiState, onHelp: () -> Unit, onNotice: () -> Unit) {
    val c = Ng.c
    Row(
        Modifier.fillMaxWidth().background(c.surface).padding(horizontal = HPad, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(14.dp))
        Text("Naerio", style = Ng.t.titleM.copy(fontWeight = FontWeight.W700, letterSpacing = (-0.02).em), color = c.ink)
        Spacer(Modifier.width(16.dp))
        if (s.data?.dataKind == DataKind.FICTIONAL_SAMPLE)
            PleosButton("가상 데이터", onNotice, kind = BtnKind.GHOST, height = TouchMin.dp, icon = Icons.Rounded.Info)
        Spacer(Modifier.weight(1f))
        PleosSegmented(
            options = listOf(
                SegOption(GatewayKind.SIMULATED, "모의 내비", Icons.Rounded.Science),
                SegOption(GatewayKind.REAL_SDK, "실제 SDK", Icons.Rounded.Navigation),
            ),
            selected = s.navKind, onSelect = vm::setNavKind,
        )
        Spacer(Modifier.width(16.dp))
        IconAction(Icons.AutoMirrored.Rounded.Help, "사용법 다시 보기", onHelp)
        val diag = s.screen == Screen.DIAGNOSTICS
        // 개발용 진단 화면은 디버그 빌드에서만 노출한다.
        if (BuildConfig.DEBUG) IconAction(
            if (diag) Icons.Rounded.Close else Icons.Rounded.Build, if (diag) "진단 닫기" else "진단",
            { vm.go(if (diag) Screen.DESTINATION else Screen.DIAGNOSTICS) },
        )
    }
    Hairline()
}

@Composable
private fun NoticeDialog(text: String, onDismiss: () -> Unit) {
    val c = Ng.c
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface, shape = CardShape,
        icon = { Icon(Icons.Rounded.Info, null, tint = c.ink2, modifier = Modifier.size(IconSize.L)) },
        title = { Text("시연용 가상 데이터", style = Ng.t.titleS, color = c.ink) },
        text = { Text(text, style = Ng.t.bodyS, color = c.ink2) },
        confirmButton = { PleosButton("확인", onDismiss, kind = BtnKind.BASIC, height = TouchMin.dp) },
    )
}

@Composable
private fun ScreenScaffold(
    title: String,
    step: Int = 0,
    summary: (@Composable RowScope.() -> Unit)? = null,
    bottom: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ng.c
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = HPad, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = Ng.t.titleL, color = c.ink)
                    summary?.let { Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically, content = it) }
                }
                if (step > 0) { Spacer(Modifier.width(24.dp)); StepBars(step, StepLabels) }
            }
            content()
        }
        Hairline()
        Row(
            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = HPad, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically, content = bottom,
        )
    }
}

@Composable
private fun TwoPane(
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
    leftWeight: Float = 1.1f,
) = BoxWithConstraints(Modifier.fillMaxWidth()) {
    if (maxWidth >= TwoPaneMin) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(leftWeight), verticalArrangement = Arrangement.spacedBy(20.dp), content = left)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = right)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), content = left)
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), content = right)
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: Int? = null) = Row(verticalAlignment = Alignment.Bottom) {
    Text(title, style = Ng.t.titleS, color = Ng.c.ink)
    count?.let { Spacer(Modifier.width(10.dp)); Text("$it", style = Ng.t.titleS.copy(fontWeight = FontWeight.W400), color = Ng.c.ink3) }
}

@Composable
private fun NextButton(text: String = "다음", enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) =
    PleosButton(text, onClick, Modifier.widthIn(min = 280.dp), enabled = enabled, icon = icon,
        trailingIcon = if (icon == null) Icons.AutoMirrored.Rounded.ArrowForward else null)

@Composable
private fun BackButton(onClick: () -> Unit, enabled: Boolean = true) =
    PleosButton("이전", onClick, Modifier.widthIn(min = 200.dp), enabled = enabled, kind = BtnKind.BASIC, icon = Icons.AutoMirrored.Rounded.ArrowBack)

@Composable
private fun DataError(errors: List<String>) = Column(
    Modifier.padding(HPad).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    Text("장소 데이터를 사용할 수 없습니다", style = Ng.t.titleL, color = Ng.c.ink)
    errors.forEach { Banner(it, BannerKind.WARN) }
}

/** 이름에서 단독 영문 대문자(B, P 등)를 표식으로 쓴다. 없으면 첫 글자. */
private fun markOf(p: Point?): String =
    p?.name?.split(" ")?.firstOrNull { it.length == 1 && it[0] in 'A'..'Z' } ?: p?.name?.take(1) ?: "?"

private fun nodesOf(p: ArrivalPlan): List<RouteNode> {
    val dest = RouteNode(NodeKind.DESTINATION, "", p.destination.name)
    return when (p.mode) {
        VisitMode.PARK_AND_VISIT -> listOf(RouteNode(NodeKind.PARKING, markOf(p.parkingEntrance), "주차"), dest)
        VisitMode.DROPOFF_AND_LEAVE -> listOf(RouteNode(NodeKind.DROPOFF, markOf(p.dropoff), "하차"), dest)
        VisitMode.DROPOFF_THEN_PARK -> listOf(
            RouteNode(NodeKind.DROPOFF, markOf(p.dropoff), "하차"),
            RouteNode(NodeKind.PARKING, markOf(p.parkingEntrance), "주차"), dest,
        )
    }
}

private fun VisitMode.glyph(): List<RouteNode> {
    val dest = RouteNode(NodeKind.DESTINATION, "", "목적지")
    return when (this) {
        VisitMode.PARK_AND_VISIT -> listOf(RouteNode(NodeKind.PARKING, "P", "주차"), dest)
        VisitMode.DROPOFF_THEN_PARK -> listOf(RouteNode(NodeKind.DROPOFF, "", "하차"), RouteNode(NodeKind.PARKING, "P", "주차"), dest)
        VisitMode.DROPOFF_AND_LEAVE -> listOf(RouteNode(NodeKind.DROPOFF, "", "하차"), dest)
    }
}

private fun VisitMode.icon(): ImageVector = when (this) {
    VisitMode.PARK_AND_VISIT -> Icons.Rounded.LocalParking
    VisitMode.DROPOFF_THEN_PARK -> Icons.Rounded.Hail
    VisitMode.DROPOFF_AND_LEAVE -> Icons.Rounded.Hail
}

// ---------------- 온보딩 ----------------

@Composable
private fun Onboarding(fictionalNotice: String?, onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = 4
    val c = Ng.c
    val (title, line) = when (page) {
        0 -> "내릴 곳부터 정해요" to "건물 앞 어디서 내리고, 어디에 주차할지 한 번에 고릅니다."
        1 -> "선과 점으로 읽어요" to "실선은 차로, 점선은 걸어서 가는 길입니다."
        2 -> "확인된 것만 믿어요" to "근거로 확인된 조건만 충족으로 표시합니다. 앱이 조건을 임의로 완화하지 않습니다."
        else -> "정차 중에 내비로 보내요" to "기존 경로를 먼저 확인하고, 보낸 뒤에는 내비 경로를 다시 읽어 반영됐는지 확인합니다."
    }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = HPad, vertical = 32.dp)) {
            val wide = maxWidth >= TwoPaneMin
            val art: @Composable () -> Unit = {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 640.dp).clip(CardShape).background(c.surface)
                        .then(if (c.isDark) Modifier.border(1.dp, c.line, CardShape) else Modifier).padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (page) {
                        0 -> OnboardRoute()
                        1 -> OnboardLegend()
                        2 -> OnboardStatus()
                        else -> OnboardApply()
                    }
                }
            }
            val words: @Composable ColumnScope.() -> Unit = {
                Text("${page + 1} / $pages", style = Ng.t.label.copy(fontWeight = FontWeight.W600), color = c.ink3)
                Text(title, style = Ng.t.headlineS, color = c.ink)
                Text(line, style = Ng.t.bodyL, color = c.ink2)
                if (page == pages - 1 && fictionalNotice != null) Banner("지금은 시연용 가상 데이터로 동작합니다. 실제 장소의 하차점이나 주차장이 아닙니다.")
            }
            if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1.15f)) { art() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = words)
            } else Column(verticalArrangement = Arrangement.spacedBy(32.dp)) { art(); Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = words) }
        }
        Hairline()
        Row(
            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = HPad, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (page > 0) BackButton({ page-- }) else PleosButton("건너뛰기", onDone, Modifier.widthIn(min = 200.dp), kind = BtnKind.GHOST)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.semantics { contentDescription = "${pages}장 중 ${page + 1}장" }) {
                repeat(pages) { i -> Box(Modifier.size(if (i == page) 14.dp else 10.dp).clip(CircleShape).background(if (i == page) c.ink else c.line)) }
            }
            Spacer(Modifier.weight(1f))
            if (page < pages - 1) NextButton { page++ } else NextButton("시작하기", icon = Icons.Rounded.Check, onClick = onDone)
        }
    }
}

@Composable
private fun OnboardRoute() = Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
    Icon(Icons.Rounded.DirectionsCar, null, tint = Ng.c.ink2, modifier = Modifier.size(96.dp))
    RouteStrip(
        listOf(RouteNode(NodeKind.DROPOFF, "", "하차"), RouteNode(NodeKind.PARKING, "P", "주차"), RouteNode(NodeKind.DESTINATION, "", "목적지")),
        node = 112.dp, segment = 150.dp,
    )
}

@Composable
private fun OnboardLegend() {
    val c = Ng.c
    @Composable
    fun LineSample(walk: Boolean) = Canvas(Modifier.width(140.dp).height(12.dp)) {
        drawLine(
            if (walk) c.accent else c.ink2, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
            strokeWidth = (if (walk) 7 else 6).dp.toPx(), cap = StrokeCap.Round,
            pathEffect = if (walk) PathEffect.dashPathEffect(floatArrayOf(0.1f, 14.dp.toPx()), 0f) else null,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { LineSample(false); Spacer(Modifier.width(24.dp)); IconText(Icons.Rounded.DirectionsCar, "차로 이동", c.ink, IconSize.M) }
        Row(verticalAlignment = Alignment.CenterVertically) { LineSample(true); Spacer(Modifier.width(24.dp)); IconText(Icons.AutoMirrored.Rounded.DirectionsWalk, "걸어서 이동", c.ink, IconSize.M) }
        Hairline()
        listOf(
            Triple(NodeKind.DROPOFF, "B", "하차점"), Triple(NodeKind.PARKING, "P", "주차 진입구"), Triple(NodeKind.DESTINATION, "", "방문할 건물"),
        ).forEach { (k, m, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(140.dp), contentAlignment = Alignment.Center) { PlaceNode(k, m, 64.dp) }
                Spacer(Modifier.width(24.dp)); Text(label, style = Ng.t.bodyL, color = c.ink)
            }
        }
    }
}

@Composable
private fun OnboardStatus() = Column(verticalArrangement = Arrangement.spacedBy(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        FactIcon(Icons.Rounded.Accessible, Tone.OK, "계단 없음", "계단 없음 확인됨", big = true)
        FactIcon(Icons.Rounded.Umbrella, Tone.UNKNOWN, "지붕", "지붕 확인 필요", big = true)
        FactIcon(Icons.Rounded.LocalParking, Tone.NO, "방문객 주차", "방문객 주차 안 됨", big = true)
    }
    Hairline()
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StatusLine(Tone.OK, "확인됨", IconSize.L)
        StatusLine(Tone.UNKNOWN, "확인 필요", IconSize.L)
        StatusLine(Tone.NO, "안 됨", IconSize.L)
    }
}

@Composable
private fun OnboardApply() = Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
    CheckRow(Icons.Rounded.Navigation, "내비 연결", Tone.OK)
    CheckRow(Icons.Rounded.DirectionsCar, "정차 중", Tone.OK)
    CheckRow(Icons.Rounded.Directions, "기존 경로 확인", Tone.OK)
    Hairline()
    CheckRow(Icons.Rounded.Sync, "보낸 뒤 다시 읽어 확인", Tone.OK)
}

// ---------------- 화면 1 목적지 ----------------

@Composable
private fun DestinationScreen(vm: AppViewModel, s: UiState) {
    val site = s.site ?: return
    ScreenScaffold(
        title = "어디로 가세요?",
        step = 1,
        summary = { IconText(Icons.Rounded.Place, site.name) },
        bottom = { Spacer(Modifier.weight(1f)); NextButton(enabled = s.destinationId != null) { vm.go(Screen.VISIT) } },
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = if (maxWidth >= TwoPaneMin) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                site.destinations.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { d ->
                            NgCard(
                                Modifier.weight(1f).heightIn(min = 320.dp),
                                selected = d.id == s.destinationId, onClick = { vm.selectDestination(d.id) }, gap = 28.dp,
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Icon(Icons.Rounded.Apartment, null, tint = Ng.c.ink3, modifier = Modifier.size(96.dp))
                                Text(d.name, style = Ng.t.headlineS, color = Ng.c.ink)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ---------------- 화면 2 방문 방식과 조건 ----------------

private fun VisitMode.label() = when (this) {
    VisitMode.PARK_AND_VISIT -> "주차하고 방문"
    VisitMode.DROPOFF_THEN_PARK -> "내려준 뒤 주차"
    VisitMode.DROPOFF_AND_LEAVE -> "내려주고 떠나기"
}

@Composable
private fun ConditionHeader(icon: ImageVector, title: String, info: String) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Ng.c.ink2, modifier = Modifier.size(IconSize.M))
            Spacer(Modifier.width(14.dp))
            Text(title, style = Ng.t.titleS, color = Ng.c.ink, modifier = Modifier.weight(1f))
            IconAction(Icons.Rounded.Info, "$title 설명", { open = !open }, selected = open)
        }
        if (open) Text(info, style = Ng.t.bodyS, color = Ng.c.ink2)
    }
}

@Composable
private fun VisitScreen(vm: AppViewModel, s: UiState) {
    val c = s.conditions
    val k = Ng.c
    val strength = listOf(SegOption(Strength.OFF, "끔"), SegOption(Strength.PREFERRED, "선호"), SegOption(Strength.REQUIRED, "필수"))
    ScreenScaffold(
        title = "어떻게 내리나요?",
        step = 2,
        summary = { IconText(Icons.Rounded.Apartment, "${s.site?.name} ${s.destination?.name ?: ""}") },
        bottom = { BackButton(vm::back); Spacer(Modifier.weight(1f)); NextButton("후보 보기", enabled = s.mode != null) { vm.go(Screen.CANDIDATES) } },
    ) {
        TwoPane(
            left = {
                VisitMode.entries.forEach { m ->
                    NgCard(selected = s.mode == m, onClick = { vm.selectMode(m) }, padding = 28.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(300.dp)) { RouteStrip(m.glyph(), segment = 44.dp, node = 56.dp) }
                            Spacer(Modifier.width(20.dp))
                            Text(m.label(), style = Ng.t.titleS, color = k.ink)
                        }
                        if (s.navKind == GatewayKind.REAL_SDK && m == VisitMode.DROPOFF_THEN_PARK && !VerifiedCapabilities.WAYPOINT_FLOW_VERIFIED)
                            IconText(Icons.Rounded.Info, "실제 내비: 경유 연결 미검증, 미리보기만")
                    }
                }
            },
            right = {
                NgCard(padding = 28.dp, gap = 12.dp) {
                    ConditionHeader(
                        Icons.Rounded.Accessible, "계단 없이",
                        "유모차나 이동 보조수단이 있으면 필수로 두세요. 계단이 없다는 사실만으로 모든 보조수단의 통과를 보장하지는 않습니다.",
                    )
                    PleosSegmented(strength, c.stepFree, { vm.setConditions(c.copy(stepFree = it)) }, Modifier.fillMaxWidth(), fill = true)
                    if (c.stepFree != Strength.OFF) PleosSegmented(
                        listOf(SegOption(AppliesTo.PASSENGER, "내리는 사람", Icons.Rounded.Person), SegOption(AppliesTo.EVERYONE, "운전자 포함", Icons.Rounded.Groups)),
                        c.stepFreeAppliesTo, { vm.setConditions(c.copy(stepFreeAppliesTo = it)) }, Modifier.fillMaxWidth(), fill = true,
                    )
                    Spacer(Modifier.height(8.dp)); Hairline(); Spacer(Modifier.height(8.dp))
                    ConditionHeader(
                        Icons.Rounded.Umbrella, "지붕 아래로",
                        "필수 조건은 확인된 경우에만 충족으로 봅니다. 선호 조건은 적격 후보 안에서 순서만 바꿉니다.",
                    )
                    PleosSegmented(strength, c.covered, { vm.setConditions(c.copy(covered = it)) }, Modifier.fillMaxWidth(), fill = true)
                }
            },
        )
    }
}

// ---------------- 화면 3 후보 비교 ----------------

private fun PlanEligibility.word() = when (this) {
    PlanEligibility.ELIGIBLE -> "조건 충족"
    PlanEligibility.NEEDS_VERIFICATION -> "확인 필요"
    PlanEligibility.INELIGIBLE -> "조건 미충족"
}

private fun PlanEligibility.tone() = when (this) {
    PlanEligibility.ELIGIBLE -> Tone.OK
    PlanEligibility.NEEDS_VERIFICATION -> Tone.UNKNOWN
    PlanEligibility.INELIGIBLE -> Tone.NO
}

private fun RequirementResult.tone() = when (this) {
    RequirementResult.SATISFIED -> Tone.OK
    RequirementResult.NOT_SATISFIED -> Tone.NO
    RequirementResult.UNVERIFIED -> Tone.UNKNOWN
    RequirementResult.NOT_APPLICABLE -> Tone.NEUTRAL
}

private fun RequirementResult.word() = when (this) {
    RequirementResult.SATISFIED -> "확인됨"
    RequirementResult.NOT_SATISFIED -> "해당 안 됨"
    RequirementResult.UNVERIFIED -> "미확인"
    RequirementResult.NOT_APPLICABLE -> "적용 안 함"
}

private fun Tone.rank() = when (this) { Tone.NO -> 3; Tone.UNKNOWN -> 2; Tone.OK -> 1; Tone.NEUTRAL -> 0 }

/** 같은 캡션의 확인 항목은 가장 나쁜 상태 하나로 합쳐 아이콘 수를 줄인다. */
private fun factsOf(p: ArrivalPlan): List<Triple<ImageVector, String, Tone>> =
    p.checks.filter { it.result != RequirementResult.NOT_APPLICABLE }
        .groupBy { checkVisual(it.label).second }
        .map { (cap, cs) -> Triple(checkVisual(cs.first().label).first, cap, cs.map { it.result.tone() }.maxBy { it.rank() }) }

private fun shortTitle(p: ArrivalPlan): String = when (p.mode) {
    VisitMode.PARK_AND_VISIT -> p.parkingFacility?.name ?: p.parkingEntrance?.name ?: "주차 진입점 없음"
    VisitMode.DROPOFF_AND_LEAVE -> p.dropoff?.name ?: "하차점 없음"
    VisitMode.DROPOFF_THEN_PARK -> "${p.dropoff?.name ?: "하차점 없음"} → ${p.parkingFacility?.name ?: p.parkingEntrance?.name ?: "주차 진입점 없음"}"
}

@Composable
private fun ConditionSummary(c: UserConditions, mode: VisitMode?) {
    mode?.let { IconText(it.icon(), it.label()) }
    if (c.stepFree != Strength.OFF) IconText(
        Icons.Rounded.Accessible,
        (if (c.stepFree == Strength.REQUIRED) "필수" else "선호") + if (c.stepFree == Strength.REQUIRED) "(${if (c.stepFreeAppliesTo == AppliesTo.PASSENGER) "동승자" else "모두"})" else "",
    )
    if (c.covered != Strength.OFF) IconText(Icons.Rounded.Umbrella, if (c.covered == Strength.REQUIRED) "필수" else "선호")
}

@Composable
private fun CandidatesScreen(vm: AppViewModel, s: UiState) {
    val ranked = vm.ranked(s) ?: return
    val k = Ng.c
    ScreenScaffold(
        title = "${s.destination?.name} 도착 후보",
        step = 3,
        summary = { ConditionSummary(s.conditions, s.mode) },
        bottom = {
            BackButton(vm::back); Spacer(Modifier.weight(1f))
            NextButton(enabled = vm.selectedPlan(s)?.eligibility == PlanEligibility.ELIGIBLE) { vm.go(Screen.CONFIRM) }
        },
    ) {
        TwoPane(
            left = {
                if (ranked.eligible.isEmpty()) {
                    NgCard {
                        StatusLine(Tone.NO, "충족하는 후보 없음", IconSize.L)
                        Text("조건을 바꾸거나 다른 방문 방식을 고르세요. 앱이 조건을 임의로 완화하지 않습니다.", style = Ng.t.bodyS, color = k.ink2)
                        PleosButton("조건 바꾸기", { vm.go(Screen.VISIT) }, kind = BtnKind.BASIC, height = TouchMin.dp, icon = Icons.Rounded.Tune)
                    }
                } else {
                    SectionLabel("추천", ranked.eligible.size)
                    if (ranked.tiedAtTop) Banner("근거가 같은 후보가 여럿입니다. 순서는 데이터 순서입니다.")
                    ranked.eligible.forEachIndexed { i, p -> PlanCard(vm, s, p, recommended = i == 0) }
                }
            },
            right = {
                if (ranked.needsVerification.isNotEmpty()) {
                    SectionLabel("확인 필요", ranked.needsVerification.size)
                    PlanList(s, ranked.needsVerification)
                }
                if (ranked.ineligible.isNotEmpty()) {
                    SectionLabel("조건 미충족", ranked.ineligible.size)
                    PlanList(s, ranked.ineligible)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanCard(vm: AppViewModel, s: UiState, p: ArrivalPlan, recommended: Boolean) {
    var expanded by rememberSaveable(p.id) { mutableStateOf(false) }
    val k = Ng.c
    NgCard(selected = s.selectedPlanId == p.id, onClick = { vm.selectPlan(p.id) }, gap = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusLine(p.eligibility.tone(), p.eligibility.word())
            if (recommended) { Spacer(Modifier.width(28.dp)); IconText(Icons.Rounded.Star, "추천", k.accent) }
        }
        RouteStrip(nodesOf(p), node = 64.dp, segment = 112.dp)
        Text(shortTitle(p), style = Ng.t.bodyL, color = k.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            factsOf(p).forEach { (icon, cap, tone) -> FactIcon(icon, tone, cap, "$cap ${when (tone) { Tone.OK -> "확인됨"; Tone.NO -> "안 됨"; else -> "확인 필요" }}") }
        }
        IconText(Icons.AutoMirrored.Rounded.DirectionsWalk, "도보 ${p.walkDistance()?.let { "${it}m" } ?: "거리 정보 없음"}")
        PleosButton(
            if (expanded) "근거 접기" else "근거 보기", { expanded = !expanded },
            kind = BtnKind.GHOST, height = TouchMin.dp, trailingIcon = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
        )
        if (expanded) Evidence(s, p)
    }
}

/** 확인 필요, 미충족 후보는 한 표면 안의 목록으로(카드 안 카드 대신 구분선). */
@Composable
private fun PlanList(s: UiState, plans: List<ArrivalPlan>) {
    val k = Ng.c
    NgCard(padding = 0.dp, gap = 0.dp) {
        plans.forEachIndexed { i, p ->
            if (i > 0) Hairline(Modifier.padding(horizontal = 24.dp))
            var open by rememberSaveable(p.id) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(p.eligibility.tone())
                    Spacer(Modifier.width(16.dp))
                    RouteStrip(nodesOf(p), node = 40.dp, segment = 28.dp, captions = false)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(shortTitle(p), style = Ng.t.bodyM.copy(fontWeight = FontWeight.W600), color = k.ink, maxLines = 1)
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            factsOf(p).filter { it.third == Tone.NO || it.third == Tone.UNKNOWN }.take(2).forEach { (icon, cap, tone) ->
                                IconText(icon, "$cap ${if (tone == Tone.NO) "안 됨" else "?"}", if (tone == Tone.NO) k.ink else k.ink3)
                            }
                        }
                    }
                    Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (open) "근거 접기" else "근거 보기", tint = k.ink3, modifier = Modifier.size(IconSize.M))
                }
                if (open) { Spacer(Modifier.height(12.dp)); Evidence(s, p) }
            }
        }
    }
}

@Composable
private fun Evidence(s: UiState, p: ArrivalPlan) {
    val k = Ng.c
    val evidence = s.data?.evidence.orEmpty().associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(roleLine(p), style = Ng.t.bodyS, color = k.ink2)
        p.checks.forEach { ch ->
            Hairline()
            Row(verticalAlignment = Alignment.Top) {
                StatusIcon(ch.result.tone(), IconSize.S)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${ch.label}${if (ch.required) " (필수)" else ""} · ${ch.result.word()}", style = Ng.t.bodyS.copy(fontWeight = FontWeight.W600), color = k.ink)
                    Text(ch.reason, style = Ng.t.bodyS, color = k.ink2)
                    ch.evidenceIds.mapNotNull { evidence[it] }.forEach { ev ->
                        Text("근거: ${ev.sourceTitle}, 확인일 ${ev.checkedAt ?: "정보 없음"}${if (ev.recheckRequired) ", 재확인 필요" else ""}", style = Ng.t.label, color = k.ink3)
                    }
                }
            }
        }
    }
}

private fun roleLine(p: ArrivalPlan): String = when (p.mode) {
    VisitMode.PARK_AND_VISIT -> "주차 진입점: ${p.parkingEntrance?.name}" + (p.parkingFacility?.let { ", 주차시설: ${it.name}" } ?: "")
    VisitMode.DROPOFF_AND_LEAVE -> "하차점: ${p.dropoff?.name}" + (p.dropoff?.description?.let { " ($it)" } ?: "")
    VisitMode.DROPOFF_THEN_PARK -> "하차점: ${p.dropoff?.name}, 이어서 주차 진입점: ${p.parkingEntrance?.name}"
}

// ---------------- 화면 4 변경 확인과 내비 반영 ----------------

@Composable
private fun RouteTimeline(plan: ArrivalPlan) {
    val k = Ng.c
    @Composable
    fun Connector(walk: Boolean) {
        val col = if (walk) k.accent else k.ink2
        Canvas(Modifier.padding(start = 29.dp).width(6.dp).height(40.dp)) {
            drawLine(
                col, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = if (walk) PathEffect.dashPathEffect(floatArrayOf(0.1f, 14.dp.toPx()), 0f) else null,
            )
        }
    }
    @Composable
    fun Stop(kind: NodeKind, mark: String, role: String, name: String) = Row(verticalAlignment = Alignment.CenterVertically) {
        PlaceNode(kind, mark, 64.dp)
        Spacer(Modifier.width(20.dp))
        Column { Text(role, style = Ng.t.label, color = k.ink3); Text(name, style = Ng.t.titleS, color = k.ink) }
    }
    Column {
        plan.navTargets.forEachIndexed { i, t ->
            if (i > 0) Connector(walk = false)
            val role = if (t.asWaypoint) "경유지(하차점)" else if (t.point.role == PointRole.DROPOFF) "목적지(하차점)" else "목적지(주차 진입점)"
            Stop(if (t.point.role == PointRole.DROPOFF) NodeKind.DROPOFF else NodeKind.PARKING, markOf(t.point), role, t.point.name)
        }
        Connector(walk = true)
        Stop(NodeKind.DESTINATION, "", "도보로 방문", plan.destination.name)
    }
}

@Composable
private fun CheckRow(icon: ImageVector, text: String, tone: Tone?, detail: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val k = Ng.c
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
        Icon(icon, null, tint = k.ink2, modifier = Modifier.size(IconSize.M))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = Ng.t.bodyM, color = k.ink)
            detail?.let { Text(it, style = Ng.t.label, color = k.ink3) }
        }
        trailing?.invoke()
        if (tone == null) CircularProgressIndicator(Modifier.size(30.dp), color = k.ink3, strokeWidth = 3.dp) else StatusIcon(tone)
    }
}

@Composable
private fun ConfirmScreen(vm: AppViewModel, s: UiState) {
    val plan = vm.selectedPlan(s) ?: run { Text("선택한 후보가 없습니다.", Modifier.padding(HPad), style = Ng.t.bodyL); return }
    val apply by vm.coordinator(s).state.collectAsStateWithLifecycle()
    val interaction by vm.policy(s).state.collectAsStateWithLifecycle()
    val connection by vm.gateway(s).connection.collectAsStateWithLifecycle()
    val unsupported = vm.unsupportedReason(plan, s)
    val ex = s.existing
    val routing = (ex.snapshot?.isRouting as? Read.Value)?.value
    val existingKnown = !ex.loading && ex.snapshot != null && routing != null
    val needsAck = routing == true
    val busy = apply.stage == ApplyStage.REQUESTING || apply.stage == ApplyStage.VERIFYING
    val appliedHere = apply.planId == plan.id && apply.stage == ApplyStage.CONFIRMED
    val canApply = unsupported == null && existingKnown && (!needsAck || s.routeChangeAcknowledged) &&
        interaction == Interaction.ALLOWED && connection is Connection.Connected && !busy && !appliedHere
    val k = Ng.c
    var simOpen by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(
        title = "내비로 보내기 전에",
        step = 4,
        summary = { IconText(Icons.Rounded.Apartment, s.destination?.name ?: ""); IconText(plan.mode.icon(), plan.mode.label()) },
        bottom = {
            BackButton({ vm.dismissApply(); vm.back() }, enabled = !busy)
            Spacer(Modifier.weight(1f))
            PleosButton(if (busy) "처리 중" else "내비에 적용", vm::applySelected, Modifier.widthIn(min = 320.dp), enabled = canApply, icon = Icons.Rounded.Navigation)
        },
    ) {
        TwoPane(
            left = {
                NgCard(gap = 20.dp) {
                    RouteTimeline(plan)
                    listOfNotNull(plan.passengerWalk?.let { "내린 뒤: ${it.guidance}" }, plan.driverWalk?.let { "주차 후: ${it.guidance}" })
                        .forEach { Row { Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, tint = k.accent, modifier = Modifier.size(IconSize.S)); Spacer(Modifier.width(10.dp)); Text(it, style = Ng.t.bodyS, color = k.ink2) } }
                }
                if (s.navKind == GatewayKind.REAL_SDK && s.data?.dataKind == DataKind.FICTIONAL_SAMPLE)
                    Banner("실제 SDK 시험: 가상 장소 대신 SDK 시험용 좌표를 보냅니다. 실제 장소의 하차점이나 주차장이 아닙니다.", BannerKind.WARN)
                if (s.navKind == GatewayKind.SIMULATED) {
                    PleosButton(
                        "시연 설정", { simOpen = !simOpen }, kind = BtnKind.GHOST, height = TouchMin.dp,
                        icon = Icons.Rounded.Tune, trailingIcon = if (simOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    )
                    if (simOpen) SimulationControls(vm, s)
                }
            },
            right = {
                NgCard(gap = 8.dp) {
                    CheckRow(
                        Icons.Rounded.Navigation,
                        when (connection) { is Connection.Connected -> "내비 연결됨"; is Connection.Connecting -> "내비 연결 중"; is Connection.Failed -> "내비 연결 안 됨" },
                        when (connection) { is Connection.Connected -> Tone.OK; is Connection.Connecting -> null; is Connection.Failed -> Tone.NO },
                        detail = (connection as? Connection.Failed)?.message,
                    )
                    Hairline()
                    CheckRow(
                        Icons.Rounded.DirectionsCar,
                        when (interaction) { Interaction.ALLOWED -> "정차 중, 조작 가능"; Interaction.RESTRICTED -> "주행 중, 적용할 수 없음"; Interaction.UNKNOWN -> "주행 상태 확인 불가, 보류" },
                        when (interaction) { Interaction.ALLOWED -> Tone.OK; Interaction.RESTRICTED -> Tone.NO; Interaction.UNKNOWN -> Tone.UNKNOWN },
                        detail = vm.policy(s).source,
                    )
                    Hairline()
                    val prefix = if (appliedHere) "적용 전: " else ""
                    when {
                        ex.loading -> CheckRow(Icons.Rounded.Directions, "기존 경로 확인 중", null)
                        ex.snapshot == null || routing == null -> CheckRow(
                            Icons.Rounded.Directions, "기존 경로 확인 안 됨, 보류", Tone.NO,
                            trailing = { IconAction(Icons.Rounded.Refresh, "다시 확인", vm::refreshExistingRoute); Spacer(Modifier.width(8.dp)) },
                        )
                        routing == false -> CheckRow(Icons.Rounded.Directions, "${prefix}안내 중인 경로 없음", Tone.OK)
                        else -> {
                            val d = (ex.snapshot.destination as? Read.Value)?.value
                            val w = (ex.snapshot.waypoints as? Read.Value)?.value
                            CheckRow(
                                Icons.Rounded.Directions,
                                "${prefix}기존 경로가 바뀝니다",
                                if (s.routeChangeAcknowledged) Tone.OK else Tone.UNKNOWN,
                                detail = "목적지 ${d?.name ?: if (ex.snapshot.destination is Read.NoResponse) "확인 불가" else "없음"} · 경유지 ${w?.let { if (it.isEmpty()) "없음" else it.joinToString { p -> p.name ?: "이름 없음" } } ?: "확인 불가"}",
                            )
                            if (!appliedHere) Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .clickable { vm.acknowledgeRouteChange(!s.routeChangeAcknowledged) }.heightIn(min = TouchMin.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = s.routeChangeAcknowledged, onCheckedChange = vm::acknowledgeRouteChange)
                                Text("기존 경로가 바뀌는 것을 확인했습니다", style = Ng.t.bodyM, color = k.ink)
                            }
                        }
                    }
                }
                unsupported?.let { Banner(it, BannerKind.WARN) }
                ApplyStatus(vm, apply, plan)
            },
        )
    }
}

@Composable
private fun ApplyStatus(vm: AppViewModel, a: ApplyState, plan: ArrivalPlan) {
    if (a.stage == ApplyStage.IDLE || a.planId != plan.id) return
    val (title, tone) = when (a.stage) {
        ApplyStage.REQUESTING -> "내비에 보내는 중" to null
        ApplyStage.VERIFYING -> "반영 확인 중" to null
        ApplyStage.CONFIRMED -> "내비 반영 확인" to Tone.OK
        ApplyStage.FAILED -> "요청 실패" to Tone.NO
        ApplyStage.UNCONFIRMED -> "반영 확인 필요" to Tone.UNKNOWN
        ApplyStage.UNSUPPORTED -> "지원하지 않는 환경" to Tone.NO
        ApplyStage.RESTRICTED -> "주행 제한으로 보류" to Tone.NO
        ApplyStage.IDLE -> "" to Tone.NEUTRAL
    }
    val k = Ng.c
    NgCard(gap = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tone == null) CircularProgressIndicator(Modifier.size(IconSize.L), color = k.ink2, strokeWidth = 4.dp) else StatusIcon(tone, IconSize.XL)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, style = Ng.t.titleS, color = k.ink)
                Text(a.message, style = Ng.t.bodyS, color = k.ink2)
            }
        }
        a.detail.forEach { Text(it, style = Ng.t.label, color = k.ink3) }
        if (a.stage in setOf(ApplyStage.FAILED, ApplyStage.UNCONFIRMED, ApplyStage.RESTRICTED, ApplyStage.UNSUPPORTED))
            Text("다시 시도하면 현재 경로와 연결 상태부터 확인합니다. 기존 경로를 자동으로 취소하지 않습니다.", style = Ng.t.label, color = k.ink3)
        if (a.stage == ApplyStage.UNCONFIRMED)
            PleosButton("현재 경로 다시 확인", vm::refreshExistingRoute, kind = BtnKind.BASIC, height = TouchMin.dp, icon = Icons.Rounded.Refresh)
    }
}

@Composable
private fun SimulationControls(vm: AppViewModel, s: UiState) {
    val sim by vm.simPolicy.state.collectAsStateWithLifecycle()
    val k = Ng.c
    NgCard(padding = 24.dp, gap = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Navigation, "내비 응답", tint = k.ink2, modifier = Modifier.size(IconSize.M)); Spacer(Modifier.width(16.dp))
            PleosSegmented(
                listOf(
                    SegOption(SimulatedNavigationGateway.Outcome.SUCCESS, "성공"),
                    SegOption(SimulatedNavigationGateway.Outcome.ERROR, "오류"),
                    SegOption(SimulatedNavigationGateway.Outcome.NO_RESPONSE, "무응답"),
                    SegOption(SimulatedNavigationGateway.Outcome.DROP_WAYPOINT, "경유지 누락"),
                ), s.simOutcome, vm::setSimOutcome, Modifier.weight(1f), fill = true,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DirectionsCar, "차량 상태", tint = k.ink2, modifier = Modifier.size(IconSize.M)); Spacer(Modifier.width(16.dp))
            PleosSegmented(
                listOf(SegOption(Interaction.ALLOWED, "주차"), SegOption(Interaction.RESTRICTED, "주행"), SegOption(Interaction.UNKNOWN, "정보 없음")),
                sim, vm.simPolicy::set, Modifier.weight(1f), fill = true,
            )
        }
        PleosButton("모의 내비 경로 비우기", vm::resetSimRoute, kind = BtnKind.BASIC, height = TouchMin.dp, icon = Icons.Rounded.Refresh)
    }
}

// ---------------- 진단: 실제 SDK 실험 ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticsScreen(vm: AppViewModel, s: UiState) {
    val log by vm.diag.collectAsStateWithLifecycle()
    val ux by vm.realInteraction.collectAsStateWithLifecycle()
    val k = Ng.c
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = HPad, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("개발용 진단: 실제 NaviHelper 실험", style = Ng.t.titleL, color = k.ink)
        Banner("SDK 시험용 좌표를 실제 Pleos 내비에 보냅니다. 실제 장소의 하차점이 아닙니다.", BannerKind.WARN)
        CheckRow(
            Icons.Rounded.DirectionsCar, "주행 제한(CarUxRestrictions): $ux",
            when (ux) { Interaction.ALLOWED -> Tone.OK; Interaction.RESTRICTED -> Tone.NO; else -> Tone.UNKNOWN },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PleosButton("연결 확인", vm::diagConnect, kind = BtnKind.BASIC, height = TouchMin.dp, icon = Icons.Rounded.Link)
            PleosButton("현재 경로 조회", { vm.diagRead() }, kind = BtnKind.BASIC, height = TouchMin.dp, icon = Icons.Rounded.Directions)
            PleosButton("A 단일 요청", { vm.diagRequest(listOf("drop_a")) }, height = TouchMin.dp)
            PleosButton("B 단일 요청", { vm.diagRequest(listOf("drop_b")) }, height = TouchMin.dp)
            PleosButton("B 경유 P 목적지", { vm.diagRequest(listOf("drop_b", "park_p_ent")) }, height = TouchMin.dp)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(k.sunken).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (log.isEmpty()) Text("기록 없음", style = Ng.t.label, color = k.ink3)
            log.reversed().forEach { Text(it, style = Ng.t.label, color = k.ink2) }
        }
    }
}
