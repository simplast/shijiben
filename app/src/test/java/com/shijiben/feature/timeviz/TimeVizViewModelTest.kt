package com.shijiben.feature.timeviz

import com.google.common.truth.Truth.assertThat
import com.shijiben.feature.recording.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * TimeVizViewModel 单测。直接 new VM 传入 fake TimeVizPrefs（不用 Hilt）。
 * 覆盖 spec 测试清单第 16-23 条 + M1 时区 + M2 clock 注入。
 *
 * 时区固定为 UTC，使 yearsLived 断言与运行机器时区无关。
 * fixedClock 与 fixedNow 指向同一时刻（2026-06-28 12:00 UTC），基准统一。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimeVizViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule()

    private var originalTz: TimeZone? = null

    private lateinit var fakePrefs: FakeTimeVizPrefs
    private lateinit var vm: TimeVizViewModel

    /** 固定 now：2026-06-28 12:00:00 UTC，使 yearsLived 断言可预测。 */
    private val fixedNow: Long = LocalDateTime.of(2026, 6, 28, 12, 0, 0)
        .atZone(ZoneId.of("UTC"))
        .toInstant()
        .toEpochMilli()

    /** M2：固定 Clock，与 fixedNow 同时刻。替代墙钟，消除时间炸弹。 */
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-06-28T12:00:00Z"), ZoneOffset.UTC
    )

    @Before
    fun setup() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        fakePrefs = FakeTimeVizPrefs()
        vm = TimeVizViewModel(fakePrefs, fixedClock)
    }

    @After
    fun teardown() {
        originalTz?.let { TimeZone.setDefault(it) }
    }

    // ===== 第 16 条：初始状态 =====
    @Test
    fun initialState_prefsEmpty_lifeResultNullAndDefaults() {
        val state = vm.state.value
        assertThat(state.birthdayMillis).isEqualTo(0L)
        assertThat(state.lifespanYears).isEqualTo(80)
        assertThat(state.lifeResult).isNull()
        assertThat(state.todayRemaining).matches("\\d+h \\d+m")
        assertThat(state.yearRemaining).matches("今年还有 \\d+ 天 \\d+ 小时")
    }

    // ===== 第 17 条：setBirthday 正常 =====
    @Test
    fun setBirthday_normal_updatesStateAndPersists() {
        vm.setBirthday(1993, 6, 28)
        // setBirthday 内部用 System.currentTimeMillis() 调 refresh；用 fixedNow 覆盖以精确断言
        vm.refresh(fixedNow)

        val state = vm.state.value
        assertThat(state.birthdayMillis).isGreaterThan(0L)
        assertThat(state.lifeResult).isNotNull()
        assertThat(state.lifeResult!!.yearsLived).isEqualTo(33)
        assertThat(state.lifeResult!!.yearsRemaining).isEqualTo(47)
        // fake prefs 中存的值与 state 暴露的一致
        assertThat(fakePrefs.getBirthdayMillis()).isEqualTo(state.birthdayMillis)
    }

    // ===== 第 18 条：setBirthday 未来日期，静默忽略（M2-1：用 fixedClock 替代墙钟） =====
    @Test
    fun setBirthday_future_silentlyIgnored() {
        // 先设一个有效生日
        vm.setBirthday(1993, 6, 28)
        val beforeBirthday = vm.state.value.birthdayMillis
        assertThat(beforeBirthday).isGreaterThan(0L)

        // 尝试设未来生日（fixedClock = 2026-06-28，2030-01-01 是未来，无墙钟依赖）
        vm.setBirthday(2030, 1, 1)
        val state = vm.state.value
        // 状态不变：birthdayMillis 仍为上一设定值
        assertThat(state.birthdayMillis).isEqualTo(beforeBirthday)
        // prefs 也没被改写
        assertThat(fakePrefs.getBirthdayMillis()).isEqualTo(beforeBirthday)
    }

    // ===== 第 19 条：setLifespan 正常 =====
    @Test
    fun setLifespan_normal_updatesState() {
        vm.setBirthday(1993, 6, 28)
        vm.refresh(fixedNow)
        val beforeYearsRemaining = vm.state.value.lifeResult!!.yearsRemaining

        vm.setLifespan(100)
        vm.refresh(fixedNow)

        val state = vm.state.value
        assertThat(state.lifespanYears).isEqualTo(100)
        // yearsRemaining 应相对 before 增加 20（lifespan +20，yearsLived 不变）
        assertThat(state.lifeResult!!.yearsRemaining).isEqualTo(beforeYearsRemaining + 20)
        // prefs 也被写入
        assertThat(fakePrefs.getLifespanYears()).isEqualTo(100)
    }

    // ===== 第 20 条：setLifespan 低于 60 clamp 到 60 =====
    @Test
    fun setLifespan_belowMin_clampsTo60() {
        vm.setLifespan(30)
        assertThat(vm.state.value.lifespanYears).isEqualTo(60)
        assertThat(fakePrefs.getLifespanYears()).isEqualTo(60)
    }

    // ===== 第 21 条：setLifespan 高于 120 clamp 到 120 =====
    @Test
    fun setLifespan_aboveMax_clampsTo120() {
        vm.setLifespan(200)
        assertThat(vm.state.value.lifespanYears).isEqualTo(120)
        assertThat(fakePrefs.getLifespanYears()).isEqualTo(120)
    }

    // ===== 第 22 条：setLifespan 幂等 =====
    @Test
    fun setLifespan_sameValue_isIdempotent() {
        vm.setLifespan(80)
        val first = vm.state.value.lifespanYears
        vm.setLifespan(80)
        val second = vm.state.value.lifespanYears
        assertThat(second).isEqualTo(first)
        assertThat(second).isEqualTo(80)
        assertThat(fakePrefs.getLifespanYears()).isEqualTo(80)
    }

    // ===== 第 23 条：refresh(fixedNow) 格式正确 =====
    @Test
    fun refresh_withFixedNow_producesFormattedStrings() {
        vm.refresh(fixedNow)
        val state = vm.state.value
        // todayRemaining 格式："Xh Ym"
        assertThat(state.todayRemaining).matches("\\d+h \\d+m")
        // yearRemaining 格式："今年还有 X 天 Y 小时"
        assertThat(state.yearRemaining).matches("今年还有 \\d+ 天 \\d+ 小时")
    }

    // ===== M1-1：时区 UTC-5（America/New_York）下 setBirthday yearsLived 与 UTC 一致 =====
    // 修复前 setBirthday 存 UTC 00:00，lifeRemaining 用 systemDefault 读 → 负偏移时区
    // 生日错位前一天。修复后统一当地 00:00 基准，birthDate 正确为生日当天。
    @Test
    fun setBirthday_utcMinus5_yearsLivedConsistentWithUtc() {
        // 切到 America/New_York（夏令时 EDT = UTC-4，仍为负偏移，bug 同样可现）
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        // 在新时区下重建 vm，让 fixedClock 仍指向同一时刻
        fakePrefs = FakeTimeVizPrefs()
        vm = TimeVizViewModel(fakePrefs, fixedClock)

        vm.setBirthday(1993, 6, 28)
        vm.refresh(fixedNow)

        val state = vm.state.value
        assertThat(state.lifeResult).isNotNull()
        // 与 UTC 时区一致：yearsLived = 33（不错位成 6-27 导致少 1 年）
        assertThat(state.lifeResult!!.yearsLived).isEqualTo(33)
        // 同时验证存储的 birthdayMillis 是当地 00:00（非 UTC 00:00）
        val expectedLocalMillis = java.time.LocalDate.of(1993, 6, 28)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertThat(fakePrefs.getBirthdayMillis()).isEqualTo(expectedLocalMillis)
    }

    // ===== M1-2：时区 UTC+8 下 setBirthday yearsLived 与 UTC 一致（回归保护） =====
    // 中国 UTC+8 不受 bug 影响（UTC 00:00 → 当地同日 08:00，LocalDate 不变），
    // 但需验证修复不破坏中国时区行为。
    @Test
    fun setBirthday_utcPlus8_yearsLivedConsistentWithUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
        fakePrefs = FakeTimeVizPrefs()
        vm = TimeVizViewModel(fakePrefs, fixedClock)

        vm.setBirthday(1993, 6, 28)
        vm.refresh(fixedNow)

        val state = vm.state.value
        assertThat(state.lifeResult).isNotNull()
        assertThat(state.lifeResult!!.yearsLived).isEqualTo(33)
        // 存储的 birthdayMillis 是当地 00:00（与 UTC+8 的旧 UTC 00:00 不同）
        val expectedLocalMillis = java.time.LocalDate.of(1993, 6, 28)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertThat(fakePrefs.getBirthdayMillis()).isEqualTo(expectedLocalMillis)
    }
}

/** 内存版 TimeVizPrefs，用于测试。 */
private class FakeTimeVizPrefs : TimeVizPrefs {
    private var birthday: Long = 0L
    private var lifespan: Int = 80

    override fun getBirthdayMillis(): Long = birthday
    override fun setBirthdayMillis(millis: Long) { birthday = millis }
    override fun getLifespanYears(): Int = lifespan
    override fun setLifespanYears(years: Int) { lifespan = years }
}
