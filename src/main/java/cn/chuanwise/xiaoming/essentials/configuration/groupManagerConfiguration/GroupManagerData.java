package cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration;

import cn.chuanwise.toolkit.preservable.AbstractPreservable;
import lombok.Getter;

import java.util.*;

@Getter
public class GroupManagerData extends AbstractPreservable {
    public Map<Long, Set<String>> groupKeys = new HashMap<>();  // 关键词撤回
    public Map<Long, Set<String>> autoVerify = new HashMap<>(); // 加群自动审核
    public Map<Long, String> join = new HashMap<>();    // 迎新
}
