# Xiaoming-Essentials

本插件分为 CoreManager（内核管理器） 和 GroupManager（群聊管理器） 两部分

  ！！！请不要随意修改配置文件！！！

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
| maxMessageCache | int | 50 | 消息缓存队列的最大长度（会直接影响防撤回的效果） |
| bannedPlugins | Set<String> | 无 | 要屏蔽的插件（插件名的散列，默认为空） |
  
## GroupManager

原 [Xiaoming-Admin](https://github.com/ThymeChen/Xiaoming-Admin) 插件的功能

### 配置文件

配置文件分为 “config.json” 和 “data.json”

  > config.json
  
|  配置项  | 类型  | 默认值 | 说明 |
|  :---: | :---: | :----: | :----: |
| defaultMuteTime  | Map<Long, Integer> | <群号， 10（单位：分钟）> | 默认禁言时间，默认每个群均为10分钟 |
| ignoreUsers  | List<Long> | 无 | 要屏蔽的用户（QQ号的集合） |
| antiRecall | Map<Long, Boolean> | 无 | 是否开启防撤回（缺省时关闭），由 群号 和 Boolean 组成的键值对 |
| antiFlash | Map<Long, Boolean> | 无 | 是否开启防闪照（缺省时关闭），由 群号 和 Boolean 组成的键值对 |
| enableAutoVerify | Map<Long, Boolean> | 无 | 是否开启自动审核（缺省时关闭），由 群号 和 Boolean 组成的键值对 |
| autoReject | Map<Long, Boolean> | <群号， false> | 是否开启自动审核不通过时拒绝加群（默认每个均为群关闭），由 群号 和 Boolean 组成的键值对。若为false，申请未命中规则时bot会私发群主加群申请的详情；若为true，则直接拒绝 |
  
  
  
  > data.json
  
|  配置项  | 类型  | 默认值 | 说明 |
|  :---: | :---: | :----: | :----: |
| groupKeys | Map<Long, Set<String>> | 无 | 群聊关键词，用于撤回敏感消息+禁言触发者，由 群号 和 散列 组成的键值对（缺省或散列为空时不触发撤回+禁言） |
| autoVerify | Map<Long, Set<String>> | 无 | 自动审核，若用户加群申请中包含 散列 中任意一条规则时，自动通过申请（忽略大小写），由 群号 和 散列 组成的键值对（缺省或散列为空时会私发群主加群申请的详情） |
| join | Map<Long, String> | 无 | 迎新词，可添加图片，由 群号 和 String 组成的键值对（缺省或字符串为空时不触发迎新） |
