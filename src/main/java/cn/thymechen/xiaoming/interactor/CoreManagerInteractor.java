package cn.thymechen.xiaoming.interactor;

import cn.thymechen.xiaoming.EssentialsPlugin;
import cn.thymechen.xiaoming.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.thymechen.xiaoming.listener.CoreManagerListener;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.plugin.PluginHandler;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.io.*;
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
    public void enableClearCall(XiaoMingUser user) {
        if (coreConfig.isEnableClearCall()) {
            user.sendMessage("明确调用已经开启过了哦");
        } else {
            if (!coreConfig.getClearCall().isEmpty() && !coreConfig.getClearCall().isBlank()) {
                coreConfig.setEnableClearCall(true);
                user.sendMessage("成功启动明确调用，未来我在具备「" + coreConfig.getGroupTag() + "」" +
                        "标记的群里只会注意「" + coreConfig.getClearCall() + "」开头的消息啦");
            } else {
                user.sendMessage("以什么开头的消息需要被小明响应呢？在下面告诉小明吧");
                coreConfig.setClearCall(user.nextMessageOrExit().serialize());
                user.sendMessage("成功设置调用头为「" + coreConfig.getClearCall() + "」，" +
                        "未来我在具备「" + coreConfig.getGroupTag() + "」" +
                        "标记的群里只会注意「" + coreConfig.getClearCall() + "」开头的消息啦");

            }
            coreConfig.setEnableClearCall(true);
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(明确|clear)(调用|call)")
    @Required("essentials.core.clearCall.onlook")
    public void disableClearCall(XiaoMingUser user) {
        if (coreConfig.isEnableClearCall()) {
            user.sendMessage("当前在具备「" + coreConfig.getGroupTag() + "」" +
                    "标记的群启动了明确调用，只会注意「" + coreConfig.getClearCall() + "」开头的消息");
        } else {
            user.sendMessage("明确调用尚未启动");
        }
    }

    @Filter("(关闭|禁用|disable)(明确|clear)(调用|call)")
    @Required("essentials.core.clearCall.disable")
    public void clearCall(XiaoMingUser user) {
        if (coreConfig.isEnableClearCall()) {
            coreConfig.setEnableClearCall(false);
            user.sendMessage("成功关闭明确调用");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        } else {
            user.sendMessage("明确调用尚未启动");
        }
    }

    @Filter("(设置|修改|set|modify)(调用|call)(头|head) {r:调用头}")
    @Required("essentials.core.clearCall.modify")
    public void modifyCallHead(XiaoMingUser user, @FilterParameter("调用头") String head) {
        if (!coreConfig.isEnableClearCall()) {
            user.sendMessage("明确调用未打开");
            return;
        }

        coreConfig.setClearCall(head);
        user.sendMessage("成功修改调用头为「" + coreConfig.getClearCall() + "」");
        xiaoMingBot.getFileSaver().readyToSave(coreConfig);
    }

    @Filter("(开启|启用|打开|enable|on)(调用|响应|call)(限制|limit)")
    @Required("essentials.core.callLimit.enable")
    public void enableCallLimit(XiaoMingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            user.sendMessage("调用限制已经打开了");
        } else {
            coreConfig.getCallLimit().enableCallLimit = true;
            user.sendMessage("成功打开调用限制");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(关闭|禁用|disable)(调用|响应|call)(限制|limit)")
    @Required("essentials.core.callLimit.disable")
    public void disableCallLimit(XiaoMingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            coreConfig.getCallLimit().enableCallLimit = false;
            user.sendMessage("成功关闭调用限制");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        } else {
            user.sendMessage("调用限制未打开");
        }
    }

    @Filter("(调用|call)(限制|limit)")
    @Required("essentials.core.callLimit.info")
    public void callLimit(XiaoMingUser user) {
        if (coreConfig.getCallLimit().isEnableCallLimit()) {
            user.sendMessage("群聊中，调用冷却为 " + coreConfig.getCallLimit().getCooldown() + " 秒，" +
                    "在 " + coreConfig.getCallLimit().getPeriod() + " 秒内最多可调用 " + coreConfig.getCallLimit().getMaxCall() + " 次");
        } else
            user.sendMessage("调用限制未打开");
    }

    @Filter("(设置|set)(调用|call)(冷却|cooldown) {time}")
    @Required("essentials.core.callLimit.set.cooldown")
    public void setCallLimitCooldown(XiaoMingUser user, @FilterParameter("time") long cooldown) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (cooldown < 1) {
            user.sendMessage("「{arg.time}」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().cooldown = (int) TimeUnit.MILLISECONDS.toSeconds(cooldown);
            user.sendMessage("成功将调用冷却设置为「" + coreConfig.getCallLimit().getCooldown() + "秒」");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(设置|set)(调用|call)(周期|period) {time}")
    @Required("essentials.core.callLimit.set.period")
    public void setCallLimitPeriod(XiaoMingUser user, @FilterParameter("time") long period) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (period < 1) {
            user.sendMessage("「{arg.time}」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().period = (int) TimeUnit.MILLISECONDS.toSeconds(period);
            user.sendMessage("成功将调用周期设置为「" + coreConfig.getCallLimit().getPeriod() + "秒」");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(设置|set)(最大|max)(调用|call)(次数|count) {r:次数}")
    @Required("essentials.core.callLimit.set.maxCall")
    public void setMaxCall(XiaoMingUser user, @FilterParameter("次数") int count) {
        if (!coreConfig.getCallLimit().isEnableCallLimit())
            return;

        if (count < 1) {
            user.sendMessage("「" + count + "」不是一个有效值哦");
        } else {
            coreConfig.getCallLimit().maxCall = count;
            CoreManagerListener.callLimit.clear();
            user.sendMessage("成功将最大调用次数设置为「" + coreConfig.getCallLimit().getMaxCall() + "次」");
            xiaoMingBot.getFileSaver().readyToSave(coreConfig);
        }
    }

    @Filter("(屏蔽|ignore)(插件|plugin) {r:插件}")
    @Required("essentials.core.plugin.ban")
    public void addIgnorePlugin(XiaoMingUser user, @FilterParameter("插件") PluginHandler plugin) {
        final String name = plugin.getName();
        if (!coreConfig.getBannedPlugins().contains(name)) {
            if (!plugin.isLoaded()) {
                user.sendError("插件「" + name + "」没有被加载哦");
                return;
            }

            if (Objects.equals(getPlugin().getName(), name)) {
                user.sendMessage("确定要屏蔽「" + name + "」本身吗？这将导致插件「" + name + "」不可启用，除非更改配置文件！\n" +
                        "请在30秒内回复「确定」来屏蔽「" + name + "」插件，超时或其他回复都将取消屏蔽");
                if (Objects.equals(user.nextMessageOrExit(30000).serialize(), "确定")) {
                    user.sendMessage("即将屏蔽插件「" + name + "」");
                } else {
                    user.sendMessage("已取消屏蔽");
                    return;
                }
            }

            if (xiaoMingBot.getPluginManager().disablePlugin(plugin)) {
                coreConfig.getBannedPlugins().add(name);
                user.sendMessage("成功屏蔽插件「" + name + "」");
                xiaoMingBot.getFileSaver().readyToSave(coreConfig);
            } else
                user.sendMessage("屏蔽插件「" + name + "」失败");
        } else
            user.sendMessage("插件「" + name + "」已在屏蔽名单中");
    }

    @Filter("(取消|解除|un|cancel)(屏蔽|ignore)(插件|plugin) {r:插件}")
    @Required("essentials.core.plugin.unban")
    public void removeIgnorePlugin(XiaoMingUser user, @FilterParameter("插件") PluginHandler plugin) {
        String name = plugin.getName();
        if (coreConfig.getBannedPlugins().contains(name)) {
            if (xiaoMingBot.getPluginManager().enablePlugin(plugin)) {
                coreConfig.getBannedPlugins().remove(name);
                user.sendMessage("成功取消屏蔽插件「" + name + "」");
                xiaoMingBot.getFileSaver().readyToSave(coreConfig);
            } else
                user.sendMessage("取消屏蔽插件「" + name + "」失败");
        } else
            user.sendMessage("插件「" + name + "」不在屏蔽名单中");
    }

    @Filter("(重载|重新加载|reload)(插件|plugin) {r:插件}")
    @Required("essentials.core.plugin.reload")
    public void reloadPlugin(XiaoMingUser user, @FilterParameter("插件") String name) {
        if (!xiaoMingBot.getPluginManager().isLoaded(name))
            user.sendMessage("插件「" + name + "」未加载");
        else {
            boolean[] reloaded = new boolean[4];
            reloaded[0] = xiaoMingBot.getPluginManager().disablePlugin(name);
            reloaded[1] = xiaoMingBot.getPluginManager().unloadPlugin(name);
            reloaded[2] = xiaoMingBot.getPluginManager().loadPlugin(name);
            reloaded[3] = xiaoMingBot.getPluginManager().enablePlugin(name);

            if (reloaded[0] && reloaded[1] && reloaded[2] && reloaded[3]) {
                user.sendMessage("成功重载插件「" + name + "」");
                return;
            }
            user.sendMessage("重载插件失败! 请尝试关闭小明!");
        }
    }

    @Filter("(重载|重加载|重新加载|重启|重新启动|reload|restart)(小明|xiaoming)")
    @Required("essentials.core.reload")
    public void reload(XiaoMingUser user) {
        final Thread thread = new Thread(() -> {
            user.sendMessage("正在关闭小明...");

            File baseDir = xiaoMingBot.getWorkingDirectory();
            String system = System.getProperty("os.name");
            String[] restartWin = {"cmd", "/c", "start " + coreConfig.getRestartScript()};
            String[] restartLinux = {"/bin/sh", "-c", coreConfig.getRestartScript()};

            try {
                xiaoMingBot.stop();
                Thread.sleep(10000);

                if (system.contains("Windows")) {
                    Runtime.getRuntime().exec(restartWin, null, baseDir);
                } else {
                    Runtime.getRuntime().exec(restartLinux, null, baseDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(thread::interrupt));
    }
}
