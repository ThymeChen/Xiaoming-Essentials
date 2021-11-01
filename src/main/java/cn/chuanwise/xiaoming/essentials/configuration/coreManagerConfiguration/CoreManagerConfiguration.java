package cn.chuanwise.xiaoming.essentials.configuration.coreManagerConfiguration;

import cn.chuanwise.toolkit.preservable.AbstractPreservable;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class CoreManagerConfiguration extends AbstractPreservable {
    public boolean enableClearCall = false;
    public String clearCall = null;
    public String groupTag = "clear-call";
    public CallLimitConfiguration callLimit = new CallLimitConfiguration();

    public Set<String> bannedPlugins = new HashSet<>();
}
