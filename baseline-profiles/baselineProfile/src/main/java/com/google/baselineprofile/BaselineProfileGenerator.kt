package com.google.baselineprofile

import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // The application id for the running build variant is read from the instrumentation arguments.
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),

            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
            includeInStartupProfile = true
        ) {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll through your most important UI.

            // Start default activity for your app
            pressHome()
            startActivityAndWait()

            // TODO Write more interactions to optimize advanced journeys of your app.
            // For example:
            // 1. Wait until the content is asynchronously loaded
            // 2. Scroll the feed content
            // 3. Navigate to detail screen
            // 1. Wait until the content is asynchronously loaded.
            waitForAsyncContent()
            // 2. Scroll the feed content.
            scrollSnackListJourney()
            // 3. Navigate to detail screen.
            goToSnackDetailJourney()
            // Check UiAutomator documentation for more information how to interact with the app.
            // https://d.android.com/training/testing/other-components/ui-automator
        }
    }
    private val TAG = "Macrobenchmark"

    fun MacrobenchmarkScope.waitForAsyncContent() {
        // 1. 先等待父容器出现，增加到 10 秒以适配物理机加载
        val snackListFound = device.wait(Until.hasObject(By.res(packageName, "snack_list")), 10_000)

        if (snackListFound) {
            val contentList = device.findObject(By.res(packageName, "snack_list"))
            // 2. 使用空安全操作符，并等待子元素渲染
            val itemFound = contentList?.wait(Until.hasObject(By.res(packageName, "snack_collection")), 5_000)
            if (itemFound != true) {
                Log.w(TAG, "Content list found but snack_collection didn't appear.")
            }
        } else {
            Log.e(TAG, "Could not find snack_list within 10 seconds.")
        }
    }

    fun MacrobenchmarkScope.scrollSnackListJourney() {
        // 确保对象存在后再操作
        val snackList = device.findObject(By.res(packageName, "snack_list"))

        if (snackList != null) {
            // 设置手势边距，防止触发系统“返回”手势
            snackList.setGestureMargin(device.displayWidth / 5)
            // 使用 fling 模拟快速滑动
            snackList.fling(Direction.DOWN)
            device.waitForIdle()
        } else {
            Log.e(TAG, "Cannot scroll: snack_list is null.")
        }
    }

    fun MacrobenchmarkScope.goToSnackDetailJourney() {
        val snackList = device.findObject(By.res(packageName, "snack_list"))

        // 获取子元素列表
        val snacks = snackList?.findObjects(By.res(packageName, "snack_item"))

        if (!snacks.isNullOrEmpty()) {
            val index = (iteration ?: 0) % snacks.size
            // 执行点击
            snacks[index].click()
            // 等待列表消失，确认进入了详情页
            device.wait(Until.gone(By.res(packageName, "snack_list")), 5_000)
        } else {
            Log.e(TAG, "No snack_items found to click.")
        }
    }
}