package cn.chuanwise.xiaoming.essentials.interactor;

import cn.chuanwise.xiaoming.essentials.EssentialsPlugin;
import cn.chuanwise.xiaoming.essentials.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.plugin.PluginHandler;
import cn.chuanwise.xiaoming.user.XiaomingUser;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CoreManagerInteractor extends SimpleInteractors<EssentialsPlugin> {
    CoreManagerConfiguration coreConfig;

    @Override
    public void onRegister() {
        coreConfig = plugin.getCoreConfig();
    }

    @Filter("(开启|启用|打开|enable|on)(明确|clear)(调用|call)")
    @Required("essentials.core.clearCall.enable")
    public void enableClearCall(XiaomingUser user) {
        if (coreConfig.isEnableClearCall()) {
            user.sendMessage("明确调用已经开启过了哦");
        } else {
            if (!Objects.isNull(coreConfig.getClearCall())) {
                coreConfig.enableClearCall = true;
                user.sendMessage("成功启动明确调用，未来我在具备「" + coreConfig.getGroupTag() + "」" +
                        "标记的群里只会注意「" + coreConfig.getClearCall() + "」开头的消息啦");

                coreConfig.enableClearCall = true;
                xiaomingBot.getFileSaver().readyToSave(coreConfig);
            } else {
                user.sendMessage("以什么开头的消息需要被小明响应呢？在下面告诉小明吧");
                coreConfig.clearCall = user.nextMessageOrExit().serialize();
                user.sendMessage("成功设置明确调用头为「" + coreConfig.clearCall + "」，" +
                        "未来我在具备「" + coreConfig.getGroupTag() + "」" +
                        "标记的群里只会注意「" + coreConfig.getClearCall() + "」开头的消息啦");

                coreConfig.enableClearCall = true;
                xiaomingBot.getFileSaver().readyToSave(coreConfig);
            }
        }
    }

    @Filter("(明确|clear)(调用|call)")
    @Required("essentials.core.clearCall.onlook")
    public void disableClearCall(XiaomingUser user) {
        if (coreConfig.isEnableClearCall()) {
            user.sendMessage("当前在具备「" + coreConfig.getGroupTag() + "」" +
                    "标记的群启动了明确调用，只会注意「" + coreConfig.getClearCall() + "」开头的消息");
        } else {
            user.sendMessage("明确调用尚未启动");
        }
    }

    @Filter("(关闭|禁用|disable)(明确|clear)(调用|call)")
    @Required("essentials.core.clearCall.disable")
    public void clearCall(XiaomingUser user) {
        if (coreConfig.isEnableClearCall()) {
            coreConfig.enableClearCall = false;
            user.sendMessage("成功关闭明确调用");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        } else {
            user.sendMessage("明确调用尚未启动");
        }
    }

    @Filter("(设置|修改|set|modify)(调用|call)(头|head) {r:调用头}")
    @Required("essentials.core.clearCall.modify")
    public void modifyCallHead(XiaomingUser user, @FilterParameter("调用头") String head) {
        if (!coreConfig.isEnableClearCall()) {
            user.sendMessage("明确调用未打开");
            return;
        }

        coreConfig.clearCall = head;
        user.sendMessage("成功修改调用头为「" + coreConfig.getClearCall() + "」");
        xiaomingBot.getFileSaver().readyToSave(coreConfig);
    }

    @Filter("(开启|启用|打开|enable|on)(调用|响应|call)(限制|limit)")
    @Required("essentials.core.callLimit.enable")
    public void enableCallLimit(XiaomingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            user.sendMessage("调用限制已经打开了");
        } else {
            coreConfig.getCallLimit().enableCallLimit = true;
            user.sendMessage("成功打开调用限制");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(关闭|禁用|disable)(调用|响应|call)(限制|limit)")
    @Required("essentials.core.callLimit.disable")
    public void disableCallLimit(XiaomingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            coreConfig.getCallLimit().enableCallLimit = false;
            user.sendMessage("成功关闭调用限制");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        } else {
            user.sendMessage("调用限制未打开");
        }
    }

    @Filter("(调用|call)(限制|limit)")
    @Required("essentials.core.callLimit.info")
    public void callLimit(XiaomingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            user.sendMessage("群聊中，调用冷却为 " + coreConfig.getCallLimit().getCooldown() + " 秒，" +
                    "在 " + coreConfig.getCallLimit().getPeriod() + " 秒内最多可调用 " + coreConfig.getCallLimit().getMaxCall() + " 次");
        } else
            user.sendMessage("调用限制未打开");
    }

    @Filter("(设置|set)(调用|call)(冷却|cooldown) {time}")
    @Required("essentials.core.callLimit.set.cooldown")
    public void setCallLimitCooldown(XiaomingUser user, @FilterParameter("time") long cooldown) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (cooldown < 1) {
            user.sendMessage("「{arg.time}」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().cooldown = (int) TimeUnit.MILLISECONDS.toSeconds(cooldown);
            user.sendMessage("成功将调用冷却设置为「" + coreConfig.getCallLimit().getCooldown() + "秒」");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(设置|set)(调用|call)(周期|period) {time}")
    @Required("essentials.core.callLimit.set.period")
    public void setCallLimitPeriod(XiaomingUser user, @FilterParameter("time") long period) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (period < 1) {
            user.sendMessage("「{arg.time}」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().period = (int) TimeUnit.MILLISECONDS.toSeconds(period);
            user.sendMessage("成功将调用周期设置为「" + coreConfig.getCallLimit().getPeriod() + "秒」");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(设置|set)(最大|max)(调用|call)(次数|count) {r:次数}")
    @Required("essentials.core.callLimit.set.maxCall")
    public void setMaxCall(XiaomingUser user, @FilterParameter("次数") int count) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (count < 1) {
            user.sendMessage("「" + count + "」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().maxCall = count;
            user.sendMessage("成功将最大调用次数设置为「" + coreConfig.getCallLimit().getMaxCall() + "次」");
            xiaomingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(屏蔽|ignore)(插件|plugin) {r:插件}")
    @Required("essentials.core.ignore.add")
    public void addIgnorePlugin(XiaomingUser user, @FilterParameter("插件") PluginHandler plugin) {
        final String name = plugin.getName();
        if (!coreConfig.getBannedPlugins().contains(name)) {
            if (!plugin.isLoaded()) {
                user.sendError("插件「" + name + "」没有被加载哦");
                return;
            }

            if (Objects.equals(getPlugin().getName(), name)) {
                user.sendMessage("确定要屏蔽「" + name + "」本身吗？这将导致插件「" + name + "」不可启用，除非更改配置文件！\n" +
                        "请在10秒内回复「确定」来屏蔽「" + name + "」插件，超时或其他回复都将取消屏蔽");
                String reply = user.nextMessageOrExit(10000).serialize();
                if (reply.equals("确定")) {
                    user.sendMessage("即将屏蔽插件「" + name + "」");
                } else {
                    user.sendMessage("已取消屏蔽");
                    return;
                }
            }

            if (xiaomingBot.getPluginManager().disablePlugin(plugin)) {
                coreConfig.getBannedPlugins().add(name);
                user.sendMessage("成功屏蔽插件「" + name + "」");
                xiaomingBot.getFileSaver().readyToSave(coreConfig);
            } else
                user.sendMessage("屏蔽插件「" + name + "」失败");
        } else
            user.sendMessage("插件「" + name + "」已被屏蔽");
    }

    @Filter("(取消|解除|un)(屏蔽|ignore)(插件|plugin) {r:插件}")
    @Required("essentials.core.ignore.remove")
    public void removeIgnorePlugin(XiaomingUser user, @FilterParameter("插件") PluginHandler plugin) {
        String name = plugin.getName();
        if (coreConfig.getBannedPlugins().contains(name)) {
            if (xiaomingBot.getPluginManager().enablePlugin(plugin)) {
                coreConfig.getBannedPlugins().remove(name);
                user.sendMessage("成功取消屏蔽插件「" + name + "」");
                xiaomingBot.getFileSaver().readyToSave(coreConfig);
            } else
                user.sendMessage("取消屏蔽插件「" + name + "」失败");
        } else
            user.sendMessage("插件「" + name + "」未被屏蔽");
    }
}
