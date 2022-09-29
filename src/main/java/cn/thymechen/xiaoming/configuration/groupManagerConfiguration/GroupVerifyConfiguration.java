package cn.thymechen.xiaoming.configuration.groupManagerConfiguration;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GroupVerifyConfiguration {
    public int verifyLength = 4;    // 验证码长度
    public String verifyCode = "23456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";    // 组成验证码的字符
    public Map<Long, Boolean> joinVerify = new HashMap<>(); // 入群验证S
}
