import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fluwx/src/models/wechat_share_models.dart';

class Fluwx {
  static const Map<Type, String> _shareModelMethodMapper = {
    WeChatShareTextModel: "shareText",
    WeChatShareImageModel: "shareImage",
    WeChatShareMusicModel: "shareMusic",
    WeChatShareVideoModel: "shareVideo",
    WeChatShareWebPageModel: "shareWebPage",
    WeChatShareMiniProgramModel: "shareMiniProgram"
  };

  static const MethodChannel _channel = const MethodChannel('fluwx');

  StreamController<Map> _responseStreamController =
      new StreamController.broadcast();

  Stream<Map> get weChatResponseUpdate => _responseStreamController.stream;

  static Future init(String appId) async {
    return await _channel.invokeMethod("initWeChat", appId);
  }

  void listen() {
    _channel.setMethodCallHandler(_handler);
  }

  void dispose() {
    _responseStreamController.close();
  }

  Future share(WeChatShareModel model) async {
    var s = _shareModelMethodMapper[model.runtimeType];
    if (_shareModelMethodMapper.containsKey(model.runtimeType)) {
      return await _channel.invokeMethod(
          _shareModelMethodMapper[model.runtimeType], model.toMap());
    } else {
      return Future.error("no method mapper found[${model.runtimeType}]");
    }
  }

  Future<dynamic> _handler(MethodCall methodCall) {
    if ("onWeChatResponse" == methodCall.method) {
      _responseStreamController.add(methodCall.arguments);
    }

    return Future.value(true);
  }
}
