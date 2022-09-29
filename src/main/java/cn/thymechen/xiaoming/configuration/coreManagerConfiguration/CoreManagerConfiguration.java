package cn.thymechen.xiaoming.configuration.coreManagerConfiguration;

import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import cn.thymechen.xiaoming.EssentialsPlugin;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public class CoreManagerConfiguration extends SimplePreservable<EssentialsPlugin> {
    String restartScript = "";       // 重启脚本
    boolean selfInteract = false;    // 是否响应自身发出的指令
    boolean enableClearCall = false; // 是否启用明确
    String clearCall = "";         // 调用头
    String groupTag = "clear-call";  // 启用的群聊标签
    CallLimitConfiguration callLimit = new CallLimitConfiguration(); // 调用限制相关
    int maxMessagesCache = 50;       // 最大消息缓存
    Set<String> bannedPlugins = new HashSet<>(); // 屏蔽的插件
}
