# Carrot Guard

一个用 Java 从零开始构建的 2D 塔防游戏原型，玩法灵感来自“保卫萝卜”这类轻量塔防，但素材、代码和设定从头原创。

## 当前版本

- Java 17
- Swing / Java2D 渲染
- Maven 项目结构
- 敌人沿固定路线移动
- 点击空地建造基础炮塔
- 炮塔自动索敌并发射子弹
- 金币、生命值、波次和游戏结束状态

## 运行

```bash
mvn package
java -jar target/carrot-guard-0.1.0-SNAPSHOT.jar
```

如果 Maven 插件下载受限，也可以直接用 JDK 编译运行：

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
java -cp out com.ktpro.carrotguard.Main
```

## 操作

- 鼠标左键点击草地格子：建造炮塔
- 每个炮塔消耗 50 金币
- 敌人抵达终点会扣生命值
- 击败敌人会获得金币

## 近期路线

- 增加不同炮塔类型
- 增加升级、出售和选中面板
- 增加关卡配置文件
- 增加怪物类型、波次编辑和地图元素
- 加入音效、贴图和主菜单

