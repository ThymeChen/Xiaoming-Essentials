package cn.thymechen.xiaoming.configuration.groupManagerConfiguration;

import cn.chuanwise.toolkit.preservable.AbstractPreservable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class GroupVerifyConfiguration {
    public int verifyLength = 4;    // 验证码长度
    public String verifyCode = "23456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";    // 组成验证码的字符
    public Map<Long, Boolean> joinVerify = new HashMap<>(); // 入群验证

    public GroupVerifyConfiguration() {

    }
}
