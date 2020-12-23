#import <Flutter/Flutter.h>
#import "FluwxMethods.h"
#import "FluwxKeys.h"
#import "FluwxWXApiHandler.h"


@class FluwxShareHandler;
@class FluwxResponseHandler;
@class FluwxAuthHandler;
@class FluwxWXApiHandler;

extern BOOL isWeChatRegistered;


@interface FluwxPlugin : NSObject<FlutterPlugin> {

@private
    FluwxShareHandler *_fluwxShareHandler;
@private
    FluwxAuthHandler *_fluwxAuthHandler;
@private FluwxWXApiHandler *_fluwxWXApiHandler;
}

@end
