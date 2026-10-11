package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BoundingBox
import com.example.data.model.RobotSettings
import com.example.data.vision.WeedDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Companion", appName)
    }

    @Test
    fun `verify bounding box center calculation`() {
        val box = BoundingBox(left = 100f, top = 200f, width = 60f, height = 80f)
        assertEquals(130f, box.xCenter, 0.01f)
        assertEquals(240f, box.yCenter, 0.01f)
    }

    @Test
    fun `verify arm kinematics within hardware bounds`() {
        val detector = WeedDetector()
        val settings = RobotSettings()
        val target = detector.calculateArmKinematics(
            xCenter = 320f,
            yCenter = 240f,
            imageWidth = 640,
            imageHeight = 480,
            settings = settings
        )

        // Center should be close to 90 degrees base
        assertTrue("Base angle should be between 80 and 100", target.baseAngle in 80..100)
        assertTrue("Shoulder angle within calibrated bounds", target.shoulderAngle in settings.armShoulderMinAngle..settings.armShoulderMaxAngle)
        assertTrue("Elbow angle within calibrated bounds", target.elbowAngle in settings.armElbowMinAngle..settings.armElbowMaxAngle)
    }
}
