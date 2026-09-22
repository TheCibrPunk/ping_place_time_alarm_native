import Flutter
import UIKit

public class PingPlaceTimeAlarmNativePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "ping_place_time_alarm_native", binaryMessenger: registrar.messenger())
    let instance = PingPlaceTimeAlarmNativePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "isNativePluginAvailable":
      result(true)
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
