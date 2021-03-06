package cn.thymechen.xiaoming.configuration.groupManagerConfiguration;

import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import cn.thymechen.xiaoming.EssentialsPlugin;
import lombok.Getter;

import java.util.*;

@Getter
public class GroupManagerConfiguration extends SimplePreservable<EssentialsPlugin> {
    public Map<Long, Integer> defaultMuteTime = new HashMap<>();   // 默认禁言时间，单位：分钟
    public List<Long> ignoreUsers = new ArrayList<>();  // 屏蔽
    public Map<Long, Boolean> antiRecall = new HashMap<>(); // 防撤回
    public Map<Long, Boolean> antiFlash = new HashMap<>();  // 防闪照
    public Map<Long, Boolean> enableAutoVerify = new HashMap<>();   // 自动审核开关
    public Map<Long, Boolean> autoReject = new HashMap<>(); // 审核不通过自动拒绝
    public Map<Long, List<Long>> muteForever = new HashMap<>();    // 永久禁言
    public GroupVerifyConfiguration verifyConfig = new GroupVerifyConfiguration();
}
