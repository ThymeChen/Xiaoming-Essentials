package cn.thymechen.xiaoming.configuration.coreManagerConfiguration;

import lombok.Getter;

@Getter
public class CallLimitConfiguration {
    public boolean enableCallLimit = false; // 调用限制开关
    public int cooldown = 5;    // 调用冷却，单位：秒
    public int period = 300;    // 调用周期，单位：秒
    public int maxCall = 10;    // 周期内最大调用次数

    public CallLimitConfiguration() {
    }
}
