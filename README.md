# 简介
一个旨在帮助快速搭建Java中小型游戏服务器的框架，通信底层采用Netty4.X，数据库采用MySql相关(框架其实并未与Mysql产生太多耦合，但建议使用Mysql)，生产消费者采用Disruptor等。此框架的作用在于共同学习，少部分经过商业验证，稳定性有一定风险，请酌情考虑。
# 模块介绍
## 网络通信(net)
关于服务器的链接过程：客户端与服务器建立Socket链接，服务器发送加密串到客户端，客户端解密后发送结果到服务器，服务器验证完毕，Socket才稳定下来，如果客户端长时间不发送正确结果，则踢掉链接。这个措施主要防止无效链接过多，增加恶意攻击服务器的成本。
## RPC服务(rpcx)
## 脚本(script)
## 消息队列(taskqueue)
## 消息队列组(taskqueuegroup)
## 数据库相关(db)
## 游戏常用集合类(collections)
## 游戏常用功能抽象(game)
## 常用工具(util)
