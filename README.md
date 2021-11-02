# Xiaoming-Essentials

本插件分为 CoreManager 和 GroupManager 两部分

## CoreManager

小明内核原有的部分功能实现
  
内核自4.0版本以来，功能大幅精简，部分原有而现在没有用的功能由本插件实现

### 配置文件

配置文件为“core.json”

  > core.json

|  配置项  | 类型  | 默认值 | 说明 |
|  :---: | :---: | :----: | :----: |
| enableClearCall  | boolean | false | 是否开启明确调用，默认为否 |
| clearCall  | String | null | 调用头，默认为null，第一次开启时会要求设置调用头 |
| groupTag | Srting | "clear-call" | 启用的群聊标签，所以带有该标签的群聊都会开启明确调用（需要将 enableClearCall 设置为 true） |
| enableCallLimit | boolean | false | 是否开启调用限制，默认为false |
| cooldown | int | 5 | 调用冷却（需要将 enableCallLimit 设置为 true） |
| period | int | 300 | 调用周期（需要将 enableCallLimit 设置为 true） |
| maxCall | int | 10 | 调用周期内允许的最大调用次数（需要将 enableCallLimit 设置为 true） |
| bannedPlugins | Set<String> | null | 要屏蔽的插件（插件名的集合，默认为空） |
  
## GroupManager

原 [Xiaoming-Admin](https://github.com/ThymeChen/Xiaoming-Admin) 插件的功能

### 配置文件

配置文件分为 “config.json” 和 “data.json”

  > config.json
|  表头  | 表头  |
|  :---: | :---: |
| 单元格  | 单元格 |
| 单元格  | 单元格 |

  > data.json
  
