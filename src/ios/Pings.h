//
//  CSignals.h
//  rf_worker
//
//  Created by 梁仲太 on 2018/7/9.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDV.h>
//测试一次
static NSInteger const ONE = 0;
//关闭测试
static NSInteger const CLOSE   = 1;
//长时测试
static NSInteger const LISTENER = 2;
//测试多次
static NSInteger const LISTENER_COUNT = 3;

//测试中
static NSInteger const PINGING = 0;
//测试完成
static NSInteger const PING_FINISH   = 1;
//测试中断
static NSInteger const PINGING_ERRO = 2;

@interface Pings : CDVPlugin

-(void)coolMethod:(CDVInvokedUrlCommand *)command;
-(void)startPing;
-(void)getPings:(BOOL)finish andCount:(NSInteger)count andPingsType:(NSInteger)type;
-(void)stopPing;
-(void)successWithMessage:(NSArray *)messages;
-(void)faileWithMessage:(NSString *)message;

@end
