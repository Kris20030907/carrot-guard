# Carrot Guard

一个用 Java 从零开始构建的 2D 塔防游戏原型，玩法灵感来自“保卫萝卜”这类轻量塔防，但素材、代码和设定从头原创。

## 当前版本

- Java 17
- Swing / Java2D 渲染
- Maven 项目结构
- 敌人沿固定路线移动
- 点击空地后可选择建造普通、减速、范围三种炮塔
- 普通、快速、重甲三种怪物
- 明确配置的 6 波关卡流程
- 障碍物占用格子，清除后奖励金币并释放建造位置
- 鼠标悬停地图时显示建造预览和攻击范围
- 点击空地选择建造位置，再点击格子旁的炮塔按钮完成建造
- 点击炮塔选中，并可选择伤害、攻速、范围三条升级分支或出售
- 炮塔自动索敌并发射子弹
- 金币、生命值、波次、暂停、重开、胜利和失败状态
- 关卡基础参数集中在 `LevelConfig`

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

## 基础检查

```bash
mvn -q -DskipTests package
javac -cp target/classes -d /tmp/carrot-guard-test-out $(find src/test/java -name "*.java")
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.GameStateSmokeCheck
```

## 操作

- 鼠标左键点击草地格子：选择建造位置
- 鼠标左键点击已有炮塔：选中炮塔
- 格子旁 Basic / Slow / Splash：在已选格子建造对应炮塔，金币不足时按钮为红色
- 鼠标移动到地图格子：预览格子是否可作为建造位置
- 顶部 Pause：暂停或继续
- 顶部 Restart：重开当前关卡
- 炮塔旁 DMG / SPD / RNG：分别升级伤害、攻速、范围
- 炮塔旁 Sell：出售选中的炮塔
- 普通炮塔价格低且输出稳定，减速炮塔会降低敌人速度，范围炮塔会造成溅射伤害
- 炮塔会优先攻击敌人，射程内没有敌人时会自动清理障碍物
- 普通怪均衡，快速怪速度高，重甲怪生命高且漏怪时扣 2 点生命
- 敌人抵达终点会扣生命值
- 击败敌人会获得金币

## 近期路线

- 增加更完整的炮塔信息面板
- 增加关卡配置文件
- 增加地图障碍物、障碍物奖励和更多关卡
- 加入音效、贴图和主菜单
