import Flutter
import UIKit
import XCTest


@testable import ping_place_time_alarm_native

// This demonstrates a simple unit test of the Swift portion of this plugin's implementation.
//
// See https://developer.apple.com/documentation/xctest for more information about using XCTest.

class RunnerTests: XCTestCase {

  func testNativePluginAvailable() {
    let plugin = PingPlaceTimeAlarmNativePlugin()

    let call = FlutterMethodCall(methodName: "isNativePluginAvailable", arguments: [])

    let resultExpectation = expectation(description: "result block must be called.")
    plugin.handle(call) { result in
      XCTAssertEqual(result as? Bool, true)
      resultExpectation.fulfill()
    }
    waitForExpectations(timeout: 1)
  }

}
