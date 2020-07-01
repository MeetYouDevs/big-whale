# Big Whale - 任务调度平台
Big Whale（巨鲸），为美柚大数据研发的大数据任务调度平台，提供Spark、Flink等离线任务的调度以及实时任务的监控，并具有批次积压报警、任务异常重启、重复应用监测、大内存应用监测等功能。
服务基于Spring Boot 2.0开发，部署方便，功能强大，打包后即可运行。[[Gitee]](https://gitee.com/progr1mmer/big-whale)

# 部署
## 1.准备
* Java 1.8+
* Mysql 5.1.0+
* 下载项目或git clone项目
* 为解决 github README.md 图片无法正常加载的问题，请在hosts文件中加入相关域名解析规则，参考：[hosts](https://github.com/MeetYouDevs/big-whale/blob/master/doc/hosts)
## 2.安装
* 创建数据库：big-whale
* 运行数据库脚本：[big_whale_tables_mysql.sql](https://github.com/MeetYouDevs/big-whale/blob/master/script/big_whale_tables_mysql.sql)
* 根据Spring Boot环境，配置相关数据库账号密码，以及SMTP信息
* 配置：[big-whale.properties](https://github.com/MeetYouDevs/big-whale/blob/master/src/main/resources/big-whale.properties)
  * 配置项说明
    * ssh.user: 拥有脚本执行权限的ssh用户（平台会使用该用户作为统一的脚本执行用户）
    * ssh.password: 拥有脚本执行权限的ssh用户密码
    * dingding.enabled: 是否开启钉钉告警
    * dingding.watcher-token: 钉钉公共群机器人Token
    * yarn.app-memory-threshold: Yarn应用内存上限，-1禁用监测
    * yarn.app-white-list: 白名单列表（列表中的应用申请的内存超过上限，不会进行报警）
* 修改：$FLINK_HOME/bin/flink（flink提交任务时，只能读取本地jar包，故需要在执行flink提交命令时从hdfs上下载jar包并替换脚本的jar包路径参数，参考：[flink](https://github.com/MeetYouDevs/big-whale/blob/master/bin/flink)）
* 服务打包：mvn clean package
## 3.启动
* 检查端口17070是否被占用，被占用的话，关闭占用的进程或修改项目端口号配置重新打包
* 拷贝target下的big-whale.jar，执行命令：java -jar big-whale.jar
## 4.初始配置
* 打开：http://localhost:17070  
  ![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step1-login.png)
* 输入账号admin，密码admin
* 点击：权限管理->用户管理，修改当前账号的邮箱为合法且存在的邮箱地址，否则会导致邮件发送错误、执行状态更新失败等问题
* 添加集群
  * 集群管理->集群管理->新增  
  ![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step2-cluster_add.png)
  * “yarn管理地址”为ResourceManager的WEB UI地址
  * 需要为Spark或Flink任务设置“程序包存储目录”，如：/data/big-whale/storage
  * “支持Flink任务代理用户”“流式任务黑名单”和“批处理任务黑名单”为内部定制的任务分配规则，可不填
* 添加设备
  * 集群管理->设备管理->新增  
  ![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step3-cluster_agent_add.png)
  * 选择集群后，会作为该集群下提交Spark或Flink任务的客户机之一（注意！！！当有多台机器属于同个集群的时，会随机选择客户机提交任务）
* 添加集群用户
  * 集群管理->集群用户->新增  
  ![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step4-cluster_cluster_user_add.png)
  * “用户”为平台用户，“代理用户”为运行Spark的代理用户，该配置的语义为：平台用户在所选集群下可使用的Yarn资源队列和代理用户（proxyuser）
* 添加计算框架版本
  * 集群管理->版本管理->新增  
  ![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step5-cluster_compute_framework_add.png)
  * 同一集群下不同版本的Spark或Flink提交任务的命令可能有所不同，如Spark：1.6.0版本的提交命令为spark-submit，2.1.0版本的提交命令为spark2-submit
# 使用
## 1.新建脚本
* 脚本管理->新增  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step6-script_add.png)
* “类型”有shell、spark实时、spark离线、flink实时、flink离线，示例为：spark实时
* 因为当前用户为超级管理员，可为所有用户创建脚本，故需要选择“用户”
* “程序包”应上传与脚本类型相应的Spark流式任务打成的jar包
* “资源选项”可不填
* 代码有两种编辑模式，“可视化视图”和“代码视图”，可互相切换  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step7-script_add_code.png)
## 2.执行脚本
* 脚本管理  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step8-script_execute.png)
* 点击执行[上一步](#1新建脚本)新建的脚本  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step9-script_execute_running.png)
* 执行成功后在详情中便可查看到Yarn应用详情的连接（提交Yarn任务的日志级别请设置为：INFO）  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step10-script_execute_success.png)
* 执行超时或失败会有相应的邮件告警  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/steperr-script_execute_timeout.png)  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/steperr-script_execute_failed.png)
## 3.实时监控
* 对于类型为“spark实时”和“flink实时”的脚本，可以通过添加此功能来实时监控任务的运行情况  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step11-script_monitor.png)
* 任务运行异常时可进行相应的处理，如批次积压报警、任务失败退出重启等  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/steperr-monitor_spark_overstock.png)  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/steperr-monitor_spark_failed.png)
## 4.离线调度
* 对于类型为“shell”、“spark离线”和“flink离线”的脚本，可通过添加此功能实现定时执行，通过添加子脚本的形式可实现串行任务调度  
![image](https://gitee.com/progr1mmer/big-whale/raw/master/doc/images/step12-script_schedule.png)  
  * 说明：平台提交saprk或flink任务的时候默认都会以“后台”的方式执行，对应spark配置：--conf spark.yarn.submit.waitAppCompletion=false，flink配置：-d，但是基于后台任务监测的实现，通过回调实现串行任务调度的时候会等待真正运行的任务完成后再执行下一脚本
## 5.Openapi
 * /openapi/script/execute.api 可执行携带可变参数的脚本
 * /openapi/scheduling/execute.api 可执行携带可变参数脚本的“离线调度”任务
 * 请求方式: POST Body
 * 参数:
   * sign: 用户密码Base64
   * id: 脚本ID或离线调度ID
   * args: 参数对象
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
