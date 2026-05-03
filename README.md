# Carrot Guard

一个用 Java 从零开始构建的 2D 塔防游戏原型，玩法灵感来自“保卫萝卜”这类轻量塔防，但素材、代码和设定从头原创。

## 当前版本

- Java 17
- Swing / Java2D 渲染
- Maven 项目结构
- 轻量 Java2D 美术层，集中绘制地图、萝卜、炮塔、敌人、障碍物和投射物
- 可选 PNG 贴图加载器，从 `src/main/resources/assets` 读取素材并保留 Java2D 兜底
- 主菜单和关卡选择界面
- 本地进度存档，记录关卡解锁状态和每关最高星级
- 代码生成的轻量音效系统，覆盖按钮、建造、升级、命中、漏怪和结算
- 设置面板支持音效开关、音量调节和清空关卡进度
- 敌人沿固定路线移动
- 点击空地后可选择建造普通、减速、范围三种炮塔
- 普通、快速、重甲三种怪物
- 三个资源配置关卡，每关包含 6 波敌人
- 关卡数据从 `src/main/resources/levels/level*.properties` 加载，并保留内置 fallback
- 障碍物占用格子，清除后奖励金币并释放建造位置
- 鼠标悬停地图时显示建造预览和攻击范围
- 点击空地选择建造位置，再点击格子旁的炮塔按钮完成建造
- 点击炮塔选中，并可选择伤害、攻速、范围三条升级分支或出售
- 选中炮塔时显示信息面板，展示当前数值、升级收益、费用和出售价值
- 炮塔自动索敌并发射子弹
- 炮塔升级、投射物命中和范围溅射带有轻量视觉反馈
- 金币、生命值、波次、暂停、重开、胜利和失败状态
- 萝卜显示 HP 条，漏怪时弹出扣血数字，点击萝卜可查看具体 HP
- 胜利后显示通关结算，包括用时、剩余 HP、金币、漏怪数、清障数和星级
- 支持 `0.5x / 1x / 2x / 4x` 游戏速度切换
- 关卡基础参数由 `LevelConfig` 加载和校验

## 运行

```bash
mvn package
java -jar target/carrot-guard-0.1.0-SNAPSHOT.jar
```

如果 Maven 插件下载受限，也可以直接用 JDK 编译运行：

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
cp -R src/main/resources/* out/
java -cp out com.ktpro.carrotguard.Main
```

## 基础检查

```bash
mvn -q -DskipTests package
javac -cp target/classes -d /tmp/carrot-guard-test-out $(find src/test/java -name "*.java")
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.GameStateSmokeCheck
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.GamePanelRenderCheck
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.GameProgressSmokeCheck
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.SoundEffectsSmokeCheck
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.AssetStoreSmokeCheck
```

## 贴图资源

可选 PNG 素材放在 `src/main/resources/assets`。支持文件名见该目录下的 `README.md`。缺少素材时游戏会自动使用 Java2D 绘制，不会影响运行。

## 关卡配置

当前关卡配置在 `src/main/resources/levels/level1.properties`、`level2.properties` 和 `level3.properties`：

- `startingCoins` / `startingLives`：初始金币和生命
- `path`：敌人路径格子，格式为 `col,row;col,row`
- `obstacles`：障碍物，格式为 `KIND:col,row`
- `wave.N`：波次，格式为 `spawnInterval,nextWaveDelay,clearBonus,ENEMY:count;ENEMY:count`

## 操作

- 鼠标左键点击草地格子：选择建造位置
- 主菜单点击关卡卡片：进入对应关卡
- 未解锁关卡会显示 Locked，通关前一关后解锁下一关
- 主菜单或战斗 HUD 的 Settings：打开设置面板
- 鼠标左键点击已有炮塔：选中炮塔
- 鼠标左键点击萝卜：查看萝卜当前 HP
- 格子旁 Basic / Slow / Splash：在已选格子建造对应炮塔，金币不足时按钮为红色
- 鼠标移动到地图格子：预览格子是否可作为建造位置
- 顶部 Pause：暂停或继续
- 顶部 Menu：返回关卡选择
- 顶部 Restart：重开当前关卡
- 顶部 Next：胜利后进入下一关
- 顶部倍速按钮：在 `0.5x / 1x / 2x / 4x` 间循环切换
- 炮塔旁 DMG / SPD / RNG：分别升级伤害、攻速、范围
- 炮塔旁 Sell：出售选中的炮塔
- 炮塔信息面板：查看当前数值、下一次升级后的数值变化、费用和出售价值
- 普通炮塔价格低且输出稳定，减速炮塔会降低敌人速度，范围炮塔会造成溅射伤害
- 炮塔会优先攻击敌人，射程内没有敌人时会自动清理障碍物
- 普通怪均衡，快速怪速度高，重甲怪生命高且漏怪时扣 2 点生命
- 敌人抵达终点会扣生命值
- 击败敌人会获得金币
- 通关结算会根据剩余 HP 和漏怪数给出星级评价
- 本地存档默认保存在用户目录的 `.carrot-guard/progress.properties`

## 近期路线

- 增加更多关卡、关卡选择和存档进度
- 加入音效、贴图和更多关卡事件
