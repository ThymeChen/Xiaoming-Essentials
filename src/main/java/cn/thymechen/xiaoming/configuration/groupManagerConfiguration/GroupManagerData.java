package cn.thymechen.xiaoming.configuration.groupManagerConfiguration;

import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import cn.thymechen.xiaoming.EssentialsPlugin;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class GroupManagerData extends SimplePreservable<EssentialsPlugin> {
    public Map<Long, Set<String>> groupKeys = new ConcurrentHashMap<>();  // 关键词撤回
    public Map<Long, Set<String>> autoVerify = new ConcurrentHashMap<>(); // 加群自动审核
    public Map<Long, String> join = new HashMap<>();    // 迎新
}
