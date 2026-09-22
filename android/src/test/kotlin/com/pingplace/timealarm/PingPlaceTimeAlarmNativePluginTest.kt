package com.pingplace.timealarm

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test

class PingPlaceTimeAlarmNativePluginTest {
    @Test
    fun isNativePluginAvailable_returnsTrue() {
        val plugin = PingPlaceTimeAlarmNativePlugin()
        val call = MethodCall("isNativePluginAvailable", null)
        val result = mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(call, result)

        verify(result).success(true)
    }
}
