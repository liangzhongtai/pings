## Pings插件使用说明
* 版本:1.1.1

## 环境配置
* npm 4.4.1 +
* node 9.8.0 +


## 使用流程
#### 注意:
###### ios平台,Mac系统下如果以下的控制台命令遇到权限问题，可以在命令前加sudo

###### 1.进入项目的根目录，添加Ping插件:com.chinamobile.gdwy.camera
* 为项目添加Pings插件，执行:`cordova plugin add com.chinamobile.ping.pings`
* 如果要删除插件,执行:`cordova plugin add com.chinamobile.ping.pings`
* 为项目添加对应的platform平台,已添加过，此步忽略，执行:
* 安卓平台: `cordova platform add android`
* ios 平台̨:`cordova platform add ios`
* 将插件添加到对应平台,执行: `cordova build`

###### 2.在js文件中,通过以下js方法调用插件，获取照片的信息数据(Base64字符串格式)
*
```javascript
    camera: function(){
        //向native发出照相请求
        //success:成功的回调函数
        //error:失败的回调函数
        //CameraMy:插件名,固定值
        //coolMethod:插件方法，固定值
        //[0,"163.177.151.110",4]:插件方法参数，具体对应以下：
        //元素1：0:测试一次，1:关闭测试，2:长时测试(需要手动关闭),3:测试指定次数
        //元素2：测试的ip地址
        //元素3: 测试的次数，默认为1，仅对指定次数的测试有效
        //元素4：每次测试的发包数，默认为4
        cordova.exec(null,null,"Pings","coolMethod",[0,"163.177.151.110",1,4]);
    }
    
    success: function(var result){
        //ping插件的测试类型
        var pingType = result[0];
        //status:测试的状态
        //0:正在测试
        //1:测试完成
        //2:测试中断
        var statsu     = result[1];
        //实时丢包率：0-100
        var lost       = result[2];
        //实时延迟/ms,>0
        var delay      = result[3];
        //平均丢包率:0-100
        var averLost   = result[4];
        //平均延迟/ms,>0
        var averDelay  = result[5];
    }

    error: function(var result){
        //测试异常提示
        alert(result);
    }
```

## 问题反馈
  在使用中有任何问题，可以用以下联系方式.
  * 作者:梁仲太
  * 邮件:18520660170@139.com
  * 时间:2018-5-24 16:00:00
