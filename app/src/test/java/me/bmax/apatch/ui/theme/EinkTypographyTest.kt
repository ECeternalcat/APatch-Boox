package me.bmax.apatch.ui.theme

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class EinkTypographyTest {
    @Test fun bodyAndLogStylesUseReadableSizeAndSpacing() {
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(24.sp, Typography.bodyLarge.lineHeight)
        assertEquals(16.sp, Typography.bodyMedium.fontSize)
        assertEquals(24.sp, Typography.bodyMedium.lineHeight)
        assertEquals(14.sp, Typography.bodySmall.fontSize)
        assertEquals(20.sp, Typography.bodySmall.lineHeight)
    }
}
