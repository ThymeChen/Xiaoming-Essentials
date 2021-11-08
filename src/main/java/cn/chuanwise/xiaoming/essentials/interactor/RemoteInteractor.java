package cn.chuanwise.xiaoming.essentials.interactor;

import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.essentials.EssentialsPlugin;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.user.PrivateXiaomingUser;
import org.apache.commons.lang.time.StopWatch;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RemoteInteractor extends SimpleInteractors<EssentialsPlugin> {
    GroupManagerConfiguration gmConfig;
    GroupManagerData gmData;

    Optional<GroupContact> groupContact = Optional.empty();

    StopWatch watch = new StopWatch();
    private void timer() {
        xiaomingBot.getScheduler().run(() -> {
            watch.start();
            while (watch.getTime() < TimeUnit.MINUTES.toMillis(30)) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            watch.reset();
            groupContact = Optional.empty();
        });
    }

    @Override
    public void onRegister() {
        gmConfig = plugin.getGmConfig();
        gmData = plugin.getGmData();
    }

    @Filter("(连接|link) {r:群号}")
    @Filter("(远程|remote) {r:群号}")
    @Filter("(远程|remote)(连接|link) {r:群号}")
    @Required("essentials.group.remote.link")
    public void link(PrivateXiaomingUser user, @FilterParameter("群号") long groupCode) {
        if (groupContact.isPresent()) {
            user.sendMessage("已经和群聊建立了连接哦，要连接新的群聊，请先退出当前群聊");
            return;
        }
        groupContact = xiaomingBot.getContactManager().getGroupContact(groupCode);
        groupContact.ifPresentOrElse(contact -> {
            timer();
            user.sendMessage("连接成功！");
        }, () -> user.sendMessage("该群不存在或Bot未加入该群"));
    }

    @Filter("(断开|退出|取消|exist|cancel|un)(连接|link)")
    @Required("essentials.group.remote.unlink")
    public void unlink(PrivateXiaomingUser user) {
        groupContact.ifPresentOrElse(contact -> {
            long groupCode = contact.getCode();
            groupContact = Optional.empty();
            user.sendMessage("已断开与群聊「" + groupCode + "」的连接");
            watch.reset();
        }, () -> user.sendMessage("还没有连接群聊哦"));
    }

    @Filter("(查看|列出|list|info|look)(连接|link)")
    @Required("essentials.group.remote.info")
    public void info(PrivateXiaomingUser user) {
        groupContact.ifPresentOrElse(contact -> user.sendMessage("当前连接信息：\n"
                + "群名：" + contact.getAlias()
                + "\n群号：" + contact.getCode()), () -> user.sendMessage("还没有连接群聊哦"));
    }

    /*远程指令*/
    // 远程发消息
    @Filter("(发送|发|send)(消息|message) {r:消息}")
    public void sendMessage(PrivateXiaomingUser user, @FilterParameter("消息") String message) {
        groupContact.ifPresentOrElse(contact -> {
            contact.sendMessage(message);
            user.sendMessage("消息已发送");
        }, () -> user.sendMessage("还没有连接群聊哦"));
    }

    //
    @Filter("(设置|set)(默认|default)(禁言|mute)(时间|time) {time}")
    @Required("essentials.group.set.defaultTime")
    public void setDefaultMuteTime(PrivateXiaomingUser user,
                                   @FilterParameter("time") long time) {
        groupContact.ifPresentOrElse(contact -> {
            final long group = contact.getCode();
            try {
                if (time <= TimeUnit.DAYS.toMillis(30) && time >= TimeUnit.MINUTES.toMillis(1)) {
                    gmConfig.getDefaultMuteTime().put(group, (int) TimeUnit.MILLISECONDS.toMinutes(time));
                    user.sendMessage("成功设置默认禁言时间为：" + gmConfig.getDefaultMuteTime().get(group) + "分钟");
                    xiaomingBot.getFileSaver().readyToSave(gmConfig);
                } else {
                    user.sendMessage("「{arg.time}」不是一个有效值哦");
                }
            } catch (Exception e) {
                user.sendMessage("设置失败");
                e.printStackTrace();
            }
        }, () -> user.sendMessage("还没有连接群聊哦"));
    }

    // 禁言
    @Filter("(禁言|mute) {qq} {r:time}")
    @Required("essentials.group.remote.mute")
    public void mute(PrivateXiaomingUser user,
                     @FilterParameter("qq") long qq,
                     @FilterParameter("time") long time) {
        groupContact.ifPresentOrElse(contact -> contact.getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.mute(time);
                        user.sendMessage("成功禁言「{arg.qq}」" + TimeUnit.MILLISECONDS.toMinutes(time) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群")), () -> user.sendError("还没有连接群聊哦"));
    }

    @Filter("(禁言|mute) {qq} ")
    @Filter("(禁言|mute) {qq}")
    @Required("essentials.group.mute")
    public void mute(PrivateXiaomingUser user,
                     @FilterParameter("qq") long qq) {
        groupContact.ifPresentOrElse(contact -> contact.getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.mute(TimeUnit.MINUTES.toMillis(gmConfig.getDefaultMuteTime().get(contact.getCode())));
                        user.sendMessage("成功禁言「{arg.qq}」" + gmConfig.getDefaultMuteTime().get(contact.getCode()) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群")), () -> user.sendMessage("还没有连接群聊哦"));
    }

    // 解禁
    @Filter("(解除禁言|unmute) {qq} ")
    @Filter("(解除禁言|unmute) {qq}")
    @Required("essentials.group.unmute")
    public void unmute(PrivateXiaomingUser user,
                       @FilterParameter("qq") long qq) {
        groupContact.ifPresentOrElse(contact -> contact.getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.unmute();
                        user.sendMessage("成功解除禁言「{arg.qq}」");
                    } catch (Exception exception) {
                        user.sendMessage("解除禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群")), () -> user.sendMessage("还没有连接群聊哦"));
    }

    // 踢人
    @Filter("(踢|踢出|kick) {qq}")
    @Required("essentials.group.kick")
    public void kick(PrivateXiaomingUser user,
                     @FilterParameter("qq") long qq) {
        groupContact.ifPresentOrElse(contact -> contact.getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.kick("");
                        user.sendMessage("已将「{arg.qq}」移出本群");
                    } catch (Exception exception) {
                        user.sendMessage("移出失败");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群")), () -> user.sendMessage("还没有连接群聊哦"));
    }
}
