//
//  CSignals.m
//  rf_worker
//
//  Created by 梁仲太 on 2018/7/9.
//

#import "Pings.h"
#import "STDebugFoundation.h"
#import "STDPingServices.h"

@interface Pings()

@property(nonatomic,copy)NSString *callbackId;
//插件动作:0.测试一次；1.关闭测试；2.长时测试Ping;3.测试指定次数的Ping
@property(nonatomic,assign)NSInteger pingsType;
//测试ip
@property(nonatomic,strong)NSString *pingIP;
//测试次数
@property(nonatomic,assign)NSInteger pingCout;
//每次测试的发包数
@property(nonatomic,assign)NSInteger packageCout;

@property(nonatomic, strong) STDPingServices    *pingServices;
@property(nonatomic,assign)float totalLost;
@property(nonatomic,assign)float totalDelay;
@property(nonatomic,assign)float oneTotalDelay;
@property(nonatomic,assign)BOOL stop;
@property(nonatomic,assign)NSInteger nowPingCount;
@property(nonatomic,assign)NSInteger nowPKCount;
@property(nonatomic,assign)NSInteger nowLostPKCount;

@end

@implementation Pings

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    NSLog(@"Pings---cool");
    self.callbackId = command.callbackId;
    self.pingsType = [command.arguments[0] integerValue];
    self.pingIP = command.arguments[1];
    self.pingCout = 1;
    self.packageCout = 4;
    if(command.arguments.count>2){
        self.pingCout = [command.arguments[2] integerValue];
    }
    if(command.arguments.count>3){
        self.packageCout = [command.arguments[3] integerValue];
    }
    [self.pingServices cancel];
    [self startPing];
}

-(void)startPing{
    self.stop = YES;
    self.totalLost = 0;
    self.totalDelay = 0;
    
    self.nowPingCount = 0;
    self.oneTotalDelay = 0;
    if(self.pingsType==ONE){
        [self getPings:YES andCount:1 andPingsType:self.pingsType];
    }else if(self.pingsType==LISTENER){
        [self getPings:NO andCount:NSIntegerMax andPingsType:self.pingsType];
    }else if(self.pingsType==LISTENER_COUNT){
        [self getPings:NO andCount:self.pingCout andPingsType:self.pingsType];
    }
}
-(void)getPings:(BOOL)finish andCount:(NSInteger)count andPingsType:(NSInteger)type{
    __weak Pings *weakSelf = self;
    weakSelf.nowPKCount = 0;
    weakSelf.nowLostPKCount =0;
    weakSelf.oneTotalDelay = 0;
    self.pingServices = [STDPingServices startPingAddress:self.pingIP callbackHandler:^(STDPingItem *pingItem, NSArray *pingItems) {
        if(pingItem.status == STDPingStatusDidFailToSendPacket
           ||pingItem.status == STDPingStatusDidTimeout
           ||pingItem.status == STDPingStatusError){
              [weakSelf.pingServices cancel];
              [weakSelf successWithMessage:[weakSelf formatArray:type andStatus:PINGING_ERRO andLost:0 andDelay:0 andAvLost:0 andAvDelay:0]];
            
        }else if (pingItem.status==STDPingStatusDidStart
                  ||pingItem.status == STDPingStatusDidReceivePacket) {
            //[weakSelf.textView appendText:pingItem.description];
            //64 bytes from 14.215.177.38: icmp_seq=99 ttl=56 time=9.039 ms
            weakSelf.nowPKCount++;
            NSLog(@"data=%lu",pingItem.dateBytesLength);
            NSLog(@"time=%f",pingItem.timeMilliseconds);
            weakSelf.oneTotalDelay += pingItem.timeMilliseconds;
            if((weakSelf.nowPKCount+weakSelf.nowLostPKCount)==weakSelf.packageCout){
                [weakSelf.pingServices cancel];
            }
        }else if(pingItem.status == STDPingStatusDidReceiveUnexpectedPacket){
            weakSelf.nowLostPKCount ++;
            if((weakSelf.nowPKCount+weakSelf.nowLostPKCount)==weakSelf.packageCout){
                [weakSelf.pingServices cancel];
            }
        }else if(pingItem.status == STDPingStatusFinished) {
            NSString *lastResult = [STDPingItem statisticsWithPingItems:pingItems];
            NSLog(@"**结束=%@",lastResult);
            if((weakSelf.nowPKCount+weakSelf.nowLostPKCount)==0
               ||(weakSelf.nowPKCount+weakSelf.nowLostPKCount)!=weakSelf.packageCout){
                 [weakSelf successWithMessage:[weakSelf formatArray:type andStatus:PINGING_ERRO andLost:0 andDelay:0 andAvLost:0 andAvDelay:0]];
                return ;
            }
            
            //[weakSelf.textView appendText:[STDPingItem statisticsWithPingItems:pingItems]];
            weakSelf.pingServices = nil;
            weakSelf.nowPingCount++;
            float lost = 0;
            if([lastResult hasPrefix:@"received"]){
                lost = [[lastResult substringWithRange:NSMakeRange([lastResult rangeOfString:@"received"].location+10, [lastResult rangeOfString:@"%"].location)] floatValue];
            }
            NSLog(@"截取的lost=%f",lost);
            weakSelf.totalLost += lost;
            float oneAvDelay = weakSelf.oneTotalDelay/weakSelf.packageCout;
            NSLog(@"单次平均delay=%f",oneAvDelay);
            weakSelf.totalDelay += oneAvDelay;
            if(weakSelf.nowPingCount<count){
                [weakSelf successWithMessage:[weakSelf formatArray:type andStatus:PINGING andLost:lost andDelay:oneAvDelay andAvLost:weakSelf.totalLost/count andAvDelay:weakSelf.totalDelay/count]];
                [weakSelf getPings:NO andCount:count andPingsType:type];
                return ;
            }
            if(type == ONE){
                [weakSelf successWithMessage:[weakSelf formatArray:type andStatus:PING_FINISH andLost:lost andDelay:oneAvDelay andAvLost:weakSelf.totalLost/count andAvDelay:weakSelf.totalDelay/count]];
            }else if(type == LISTENER){
                
            }else if(type == LISTENER_COUNT){
                [weakSelf successWithMessage:[weakSelf formatArray:type andStatus:PING_FINISH andLost:lost andDelay:oneAvDelay andAvLost:weakSelf.totalLost/count andAvDelay:weakSelf.totalDelay/count]];
            }
        }
    }];
    
}

-(void)stopPing{
    self.stop = YES;
    [self.pingServices cancel];
}

-(NSArray *)formatArray:(NSInteger)type andStatus:(NSInteger)status andLost:(float)lost andDelay:(float)delay andAvLost:(float)avLost andAvDelay:(float)avDelay{
    return @[[NSNumber numberWithInteger:type],[NSNumber numberWithInteger:status]
             ,[NSNumber numberWithFloat:lost],[NSNumber numberWithFloat:delay]
             ,[NSNumber numberWithFloat:avLost],[NSNumber numberWithFloat:avDelay]];
}

-(void)successWithMessage:(NSArray *)messages{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    NSLog(@"message=%@",messages);
    [result setKeepCallbackAsBool:self.pingsType!=ONE];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

-(void)faileWithMessage:(NSString *)message{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

@end
