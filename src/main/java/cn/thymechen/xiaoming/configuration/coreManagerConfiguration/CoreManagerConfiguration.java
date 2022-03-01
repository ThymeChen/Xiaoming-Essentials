package cn.thymechen.xiaoming.configuration.coreManagerConfiguration;

import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import cn.thymechen.xiaoming.EssentialsPlugin;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class CoreManagerConfiguration extends SimplePreservable<EssentialsPlugin> {
    public String restartScript = "";       // 重启脚本
    public boolean selfInteract = false;    // 是否响应自身发出的指令
    public boolean enableClearCall = false; // 是否启用明确
    public String clearCall = null;         // 调用头
    public String groupTag = "clear-call";  // 启用的群聊标签
    public CallLimitConfiguration callLimit = new CallLimitConfiguration(); // 调用限制相关
    public int maxMessagesCache = 50;       // 最大消息缓存
    public Set<String> bannedPlugins = new HashSet<>(); // 屏蔽的插件
}
