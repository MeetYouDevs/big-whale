# Big Whale
巨鲸任务调度平台为美柚大数据研发的分布式计算任务调度系统，提供Spark、Flink等批处理任务的DAG执行调度和流处理任务的状态监测调度，并具有重复应用检测、大内存应用检测等功能。
服务基于Spring Boot 2.0开发，打包后即可运行。[[Github]](https://github.com/MeetYouDevs/big-whale)[[Gitee]](https://gitee.com/meetyoucrop/big-whale)

# 概述
## 1.架构图
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/architecture.png)
## 2.特性
* 基于SSH的脚本执行机制，部署简单快捷，仅需单个服务
* 基于Yarn Rest Api的任务状态同步机制，对Spark、Flink无版本限制
* 支持分布式
* 支持失败重试
* 支持任务依赖
* 支持复杂任务编排（DAG）
* 支持流处理任务运行状态监测
# 部署
## 1.准备
* Java 1.8+
* Mysql 5.1.0+
* 下载项目或git clone项目
* 为解决 github README.md 图片无法正常加载的问题，请在hosts文件中加入相关域名解析规则，参考：[hosts](https://github.com/MeetYouDevs/big-whale/blob/master/doc/hosts)
## 2.安装
* 创建数据库：big-whale
* 运行数据库脚本：[big-whale.sql](https://github.com/MeetYouDevs/big-whale/blob/master/script/big-whale.sql)
* 根据Spring Boot环境，配置相关数据库账号密码，以及SMTP信息
* 配置：[big-whale.properties](https://github.com/MeetYouDevs/big-whale/blob/master/src/main/resources/big-whale.properties)
  * 配置项说明
    * ssh.user: 拥有脚本执行权限的ssh远程登录用户名（平台会将该用户作为统一的脚本执行用户）
    * ssh.password: ssh远程登录用户密码
    * dingding.enabled: 是否开启钉钉告警
    * dingding.watcher-token: 钉钉公共群机器人Token
    * yarn.app-memory-threshold: Yarn应用内存上限（单位：MB），-1禁用检测
    * yarn.app-white-list: Yarn应用白名单列表（列表中的应用申请的内存超过上限，不会进行告警）
* 修改：$FLINK_HOME/bin/flink，参考：[flink](https://github.com/MeetYouDevs/big-whale/blob/master/bin/flink)（因flink提交任务时只能读取本地jar包，故需要在执行提交命令时从hdfs上下载jar包并替换脚本中的jar包路径参数）
* 打包：mvn clean package
## 3.启动
* 检查端口17070是否被占用，被占用的话，关闭占用的进程或修改项目端口号配置重新打包
* 拷贝target目录下的big-whale.jar，执行命令：java -jar big-whale.jar
## 4.初始配置
* 打开：http://localhost:17070  
  ![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step1-login.png)
* 输入账号admin，密码admin
* 点击：权限管理->用户管理，修改当前账号的邮箱为合法且存在的邮箱地址，否则会导致邮件发送失败
* 添加集群
  * 集群管理->集群管理->新增  
  ![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step2-cluster_add.png)
  * “yarn管理地址”为Yarn ResourceManager的WEB UI地址
  * “程序包存储目录”为程序包上传至hdfs集群时的存储路径，如：/data/big-whale/storage
  * “支持Flink任务代理用户”“流处理任务黑名单”和“批处理任务黑名单”为内部定制的任务分配规则，勿填
* 添加代理
  * 集群管理->代理管理->新增  
  ![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step3-cluster_agent_add.png)
  * 可添加多个实例（仅支持IP地址，可指定端口号，默认为22），执行脚本的时候会随机选择一个实例执行，在实例不可达的情况下，会继续随机选择下一个实例，在实例均不可达时执行失败
  * 选择集群后，会作为该集群下提交Spark或Flink任务的代理之一
* 添加集群用户
  * 集群管理->集群用户->新增  
  ![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step4-cluster_cluster_user_add.png)
  * 该配置的语义为：平台用户在所选集群下可以使用的Yarn资源队列（--queue）和代理用户（--proxy-user）
* 添加计算框架版本
  * 集群管理->版本管理->新增  
  ![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step5-cluster_compute_framework_add.png)
  * 同一集群下不同版本的Spark或Flink任务的提交命令可能有所不同，如Spark 1.6.0版本的提交命令为spark-submit，Spark 2.1.0版本的提交命令为spark2-submit
# 使用
## 1.新建脚本
* 脚本管理->新增  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step6-script_add.png)
* “类型”有shell批处理、spark流处理、spark批处理、flink流处理和flink批处理，示例为：spark流处理
* 因为当前用户为超级管理员，可为所有用户创建脚本，故可以选择“用户”
* 非“shell批处理”类型的脚本应上传与之处理类型相对应的程序包，此处为spark流处理任务打成的jar包
* “资源选项”可不填
* 代码有两种编辑模式，“可视化视图”和“代码视图”，可互相切换  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step7-script_add_code.png)
## 2.执行脚本
* 脚本管理  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step8-script_list.png)
* 点击执行[上一步](#1新建脚本)新建的脚本  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step9-script_execute_running.png)
* 执行成功后可查看Yarn应用详情连接（代理实例上Yarn任务提交命令的日志级别请设置为：INFO）  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step10-script_execute_success.png)
* 执行超时或失败会有相应的邮件告警  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/steperr-script_execute_timeout.png)  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/steperr-script_execute_failed.png)
## 3.任务调度
### 3.1 批处理
* 任务调度->新增->批处理
* 对于类型为“shell批处理”、“spark批处理”和“flink批处理”的脚本，可通过添加此功能实现DAG执行调度 (v1.1开始支持，需要从v1.0升级的，请查阅[SchedulingFieldGenerateForV1_1Test.java](https://github.com/MeetYouDevs/big-whale/blob/master/src/test/java/com/meiyou/bigwhale/test/SchedulingFieldGenerateForV1_1Test.java))
* 说明：为防止平台线程被大量占用，平台提交Saprk或Flink任务的时候都会强制以“后台”的方式执行，对应spark配置：--conf spark.yarn.submit.waitAppCompletion=false，flink配置：-d，但是基于后台“批处理应用状态更新任务”的回调，在实现DAG执行引擎时可以确保当前节点脚本所提交的批处理任务运行完成后再执行下一个节点的脚本  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step11-scheduling_batch_add.png)  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step12-scheduling_batch_list.png)
* DAG节点支持失败重试(v1.2开始支持，需要从v1.0或v1.1升级的，请查阅[SchedulingFieldGenerateForV1_2Test.java](https://github.com/MeetYouDevs/big-whale/blob/master/src/test/java/com/meiyou/bigwhale/test/SchedulingFieldGenerateForV1_2Test.java))
### 3.2 流处理
* 任务调度->新增->流处理
* 对于类型为“spark流处理”和“flink流处理”的脚本，可通过添加此功能实现状态监测调度  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step13-scheduling_streaming_add.png)  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/step14-scheduling_streaming_list.png)
* 可根据状态监测结果进行相应的处理，如异常重启、批次积压告警等  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/steperr-spark_streaming_failed.png)  
![image](https://gitee.com/meetyoucrop/big-whale/raw/master/doc/images/steperr-spark_streaming_overstock.png)
## 4.Openapi
* /openapi/script/execute.api 执行脚本
* /openapi/scheduling/execute.api 执行任务调度（批处理）
* 请求方式: POST Body
* 参数:
  * sign: 用户密码Base64
  * id: 脚本ID或离线调度ID
  * args: 脚本参数（可选）
 ```
 {
   "sign": "c3V4aWFveWFuZzExIQ==",
   "id": "8a80813a7154f28a017154f6637c1794",
   "args": {
     "$output_dir": "/var",
     "$dt": "20200415"
   }
 }
 ```
# License
The project is licensed under the [Apache 2 license](https://github.com/MeetYouDevs/big-whale/blob/master/LICENSE).
