package app.naerio

import app.naerio.data.LoadResult
import app.naerio.data.PlaceJson
import app.naerio.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PlanLogicTest {

    private val data: PlaceData by lazy {
        val f = listOf("src/main/assets/places/demo_site.json", "app/src/main/assets/places/demo_site.json").map(::File).first { it.exists() }
        (PlaceJson.parse(f.readText()) as LoadResult.Loaded).data
    }
    private val site get() = data.sites.first()
    private val build = BuildArrivalPlans()
    private val rank = RankEligiblePlans()
    private fun plans(dest: String, mode: VisitMode, c: UserConditions = UserConditions()) =
        rank.rank(build.build(site, dest, mode, c))

    @Test fun demoDataIsValid() {
        assertTrue(PlaceJson.parse(File(listOf("src/main/assets/places/demo_site.json", "app/src/main/assets/places/demo_site.json").first { File(it).exists() }).readText()) is LoadResult.Loaded)
    }

    @Test fun stepFreeRequired_onlyBEligible_AUnverified() {
        val r = plans("dest_103", VisitMode.DROPOFF_AND_LEAVE)
        assertEquals(listOf("drop_b"), r.eligible.map { it.dropoff!!.id })
        assertEquals(listOf("drop_a"), r.needsVerification.map { it.dropoff!!.id })
        val a = r.needsVerification.first()
        assertTrue(a.checks.any { it.required && it.result == RequirementResult.UNVERIFIED && "계단" in it.label })
    }

    @Test fun dropoffThenPark_BtoP_isTheOnlyEligiblePlan() {
        val r = plans("dest_103", VisitMode.DROPOFF_THEN_PARK)
        assertEquals(listOf("DROPOFF_THEN_PARK:drop_b:park_p_ent"), r.eligible.map { it.id })
        val p = r.recommended!!
        assertEquals(listOf(true, false), p.navTargets.map { it.asWaypoint })
        assertEquals(listOf("drop_b", "park_p_ent"), p.navTargets.map { it.point.id })
    }

    @Test fun missingVehicleLink_BtoS_needsVerification() {
        val r = plans("dest_103", VisitMode.DROPOFF_THEN_PARK)
        val bs = (r.needsVerification + r.ineligible).first { it.id == "DROPOFF_THEN_PARK:drop_b:park_s_ent" }
        assertEquals(PlanEligibility.NEEDS_VERIFICATION, bs.eligibility)
        assertTrue(bs.checks.any { it.label == "하차 후 주차장 이동" && it.result == RequirementResult.UNVERIFIED })
    }

    @Test fun allCandidatesUnverified_noEligible() {
        val r = plans("dest_105", VisitMode.DROPOFF_AND_LEAVE)
        assertTrue(r.eligible.isEmpty())
        assertEquals(2, r.needsVerification.size)
    }

    @Test fun recheckRequired_isNotSatisfied() {
        val r = plans("dest_105", VisitMode.DROPOFF_AND_LEAVE)
        val b = r.needsVerification.first { it.dropoff!!.id == "drop_b" }
        assertTrue(b.checks.any { "계단" in it.label && it.result == RequirementResult.UNVERIFIED && "재확인" in it.reason })
    }

    @Test fun clearlyNotSatisfied_isIneligible() {
        val r = plans("dest_107", VisitMode.DROPOFF_AND_LEAVE)
        assertTrue(r.eligible.isEmpty())
        assertEquals(setOf("drop_a", "drop_b"), r.ineligible.map { it.dropoff!!.id }.toSet())
    }

    @Test fun userTurnsOffCondition_reevaluates() {
        val r = plans("dest_103", VisitMode.DROPOFF_AND_LEAVE, UserConditions(stepFree = Strength.OFF))
        assertEquals(setOf("drop_a", "drop_b"), r.eligible.map { it.dropoff!!.id }.toSet())
    }

    @Test fun preferredDoesNotOverrideRequired() {
        // 지붕을 선호해도 계단 조건 미확인인 A가 적격이 되지 않는다.
        val r = plans("dest_103", VisitMode.DROPOFF_AND_LEAVE, UserConditions(covered = Strength.PREFERRED))
        assertEquals(listOf("drop_b"), r.eligible.map { it.dropoff!!.id })
    }

    @Test fun parking_visitorConditionsDiffer() {
        val r = plans("dest_103", VisitMode.PARK_AND_VISIT, UserConditions(stepFreeAppliesTo = AppliesTo.EVERYONE))
        assertEquals(setOf("park_p_ent", "park_s_ent"), r.eligible.map { it.parkingEntrance!!.id }.toSet())
        assertEquals(listOf("park_q_ent"), r.ineligible.map { it.parkingEntrance!!.id })
        val rPlan = r.needsVerification.first { it.parkingEntrance!!.id == "park_r_ent" }
        assertTrue(rPlan.checks.any { it.label == "방문객 주차" && "서로 달라" in it.reason })
    }

    @Test fun parkingWalkStartsFromPedestrianExit_notEntrance() {
        val p = plans("dest_103", VisitMode.PARK_AND_VISIT).eligible.first { it.parkingEntrance!!.id == "park_p_ent" }
        assertEquals("park_p_exit", p.driverWalk!!.fromPointId)
    }

    @Test fun conditionTargetDiffers_passengerOnly() {
        // 동승자에게만 적용하면 운전자의 주차 후 보행에는 필수로 적용하지 않는다.
        val p = plans("dest_103", VisitMode.DROPOFF_THEN_PARK, UserConditions(stepFreeAppliesTo = AppliesTo.PASSENGER)).recommended!!
        val driverStep = p.checks.filter { "계단" in it.label }
        assertTrue(driverStep.any { it.result == RequirementResult.NOT_APPLICABLE && !it.required })
    }

    @Test fun preferredCovered_ranksPFirstForParking() {
        val r = plans("dest_103", VisitMode.PARK_AND_VISIT, UserConditions(stepFreeAppliesTo = AppliesTo.EVERYONE, covered = Strength.PREFERRED))
        assertEquals("park_p_ent", r.recommended!!.parkingEntrance!!.id)
        assertFalse(r.tiedAtTop)
    }

    @Test fun missingDistance_staysNull() {
        val p = plans("dest_103", VisitMode.DROPOFF_AND_LEAVE).recommended!!
        assertNull(p.walkDistance())
    }

    @Test fun switchingModeRebuildsTargets() {
        val park = plans("dest_103", VisitMode.PARK_AND_VISIT, UserConditions(stepFreeAppliesTo = AppliesTo.EVERYONE)).recommended!!
        val leave = plans("dest_103", VisitMode.DROPOFF_AND_LEAVE).recommended!!
        assertEquals(PointRole.PARKING_ENTRANCE, park.navTargets.single().point.role)
        assertEquals(PointRole.DROPOFF, leave.navTargets.single().point.role)
    }

    @Test fun invalidJson_reportsErrorsWithoutCrash() {
        val bad = """{"schemaVersion":1,"dataKind":"FICTIONAL_SAMPLE","notice":"x","evidence":[],
          "sites":[{"id":"s","name":"s","siteType":"APARTMENT","destinations":[{"id":"d","name":"d"}],
          "points":[{"id":"p","role":"DROPOFF","name":"p","sdkTestCoordinate":{"latitude":95.0,"longitude":10.0},
            "vehicleAccess":{"status":"KNOWN","value":true,"evidenceIds":["missing"]}},
            {"id":"p","role":"PARKING_ENTRANCE","name":"dup","parkingFacilityId":"nope"}],
          "pedestrianConnections":[{"id":"w","fromPointId":"ghost","toDestinationId":"d","guidance":"g"}]}]}"""
        val r = PlaceJson.parse(bad)
        assertTrue(r is LoadResult.Invalid)
        val errs = (r as LoadResult.Invalid).errors.joinToString("\n")
        listOf("중복 ID", "시험 좌표 범위", "존재하지 않는 근거", "주차시설 참조", "출발 지점 없음").forEach { assertTrue(it, it in errs) }
    }

    @Test fun unknownEnum_isRejected() {
        val r = PlaceJson.parse("""{"schemaVersion":1,"dataKind":"SOMETHING","notice":"x","sites":[]}""")
        assertTrue(r is LoadResult.Invalid)
    }

    @Test fun fictionalFileWithRealCoordinate_isRejected() {
        val text = File(listOf("src/main/assets/places/demo_site.json", "app/src/main/assets/places/demo_site.json").first { File(it).exists() }).readText()
            .replaceFirst("\"sdkTestCoordinate\"", "\"coordinate\"")
        assertTrue(PlaceJson.parse(text) is LoadResult.Invalid)
    }
}
