package cn.chuanwise.xiaoming.essentials.interactor;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.MapUtil;
import cn.chuanwise.xiaoming.essentials.EssentialsPlugin;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import net.mamoe.mirai.message.code.MiraiCode;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GroupManagerInteractor extends SimpleInteractors<EssentialsPlugin> {
    GroupManagerConfiguration gmConfig;
    GroupManagerData gmData;

    Set<?> tempIndex = new HashSet<>(); // 临时集合，记录索引信息
    int count = 0;  // 记录查询后的删除次数
    Object reply;

    private <K, E> boolean removeRemainOrIndex(Set<E> set, K key, E e) {
        int index = -1;
        E remain = null;

        try {
            index = Integer.parseInt((String) e);
            remain = (E) CollectionUtil.arrayGet(tempIndex, index - 1);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (set.remove(e)) {
                count++;
                reply = remain;
                return true;
            } else {
                if (index == -1 || remain == null)
                    return false;

                if (set.remove(remain)) {
                    count++;
                    reply = remain.toString();
                    return true;
                } else
                    return false;
            }
        }
    }

    @Override
    public void onRegister() {
        gmConfig = plugin.getGmConfig();
        gmData = plugin.getGmData();
    }

    @Filter("设置默认禁言时间 {time}")
    @Required("essentials.group.set.defaultTime")
    public void setDefaultMuteTime(GroupXiaomingUser user,
                                   @FilterParameter("time") long time) {
        final long group = user.getGroupCode();

        try {
            if (time < (43200L * 60000) && time > 60000) {
                gmConfig.getDefaultMuteTime().put(group, time / 60000);
                user.sendMessage("成功设置默认禁言时间为：" + time / 60000 + "分钟");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } else {
                user.sendMessage("「{arg.time}」不是一个有效值哦");
            }
        } catch (Exception e) {
            user.sendMessage("设置失败");
            e.printStackTrace();
        }
    }

    // 禁言
    @Filter("(禁言|mute) {qq} ")
    @Filter("(禁言|mute) {qq}")
    @Required("essentials.group.mute")
    public void mute(GroupXiaomingUser user,
                     @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.mute(gmConfig.getDefaultMuteTime().get(user.getGroupCode()) * 60000);
                        user.sendMessage("成功禁言「{arg.qq}」" + gmConfig.getDefaultMuteTime().get(user.getGroupCode()) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(禁言|mute) {qq} {r:time}")
    @Required("essentials.group.mute.time")
    public void mute(GroupXiaomingUser user,
                     @FilterParameter("qq") long qq,
                     @FilterParameter("time") long time) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.mute(time);
                        user.sendMessage("成功禁言「{arg.qq}」" + TimeUnit.MILLISECONDS.toMinutes(time) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(解除禁言|unmute) {qq} ")
    @Filter("(解除禁言|unmute) {qq}")
    @Required("essentials.group.unmute")
    public void unmute(GroupXiaomingUser user,
                       @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.unmute();
                        user.sendMessage("成功解除禁言「{arg.qq}」");
                    } catch (Exception exception) {
                        user.sendMessage("解除禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    // 踢人
    @Filter("(踢|踢出|kick) {qq}")
    @Required("essentials.group.kick")
    public void kick(GroupXiaomingUser user,
                     @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        member.kick("");
                        user.sendMessage("已踢出「{arg.qq}」");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    // 屏蔽用户
    @Filter("(屏蔽|ignore)(用户|user) {qq}")
    @Filter("(屏蔽|ignore)(用户|user) {qq} ")
    @Required("essentials.group.ignore.add.user")
    public void ignoreGroups(GroupXiaomingUser user,
                             @FilterParameter("qq") long qq) {
        if (!gmConfig.getIgnoreUsers().contains(qq)) {
            try {
                gmConfig.getIgnoreUsers().add(qq);

                user.sendMessage("成功屏蔽用户「{args.qq}」");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("屏蔽失败");
            }
        } else {
            user.sendMessage("用户「{args.qq}」已在屏蔽名单中");
        }
    }

    @Filter("(取消屏蔽|解除屏蔽|unIgnore)(用户|User) {qq}")
    @Required("essentials.group.ignore.remove.user")
    public void unIgnore(GroupXiaomingUser user,
                         @FilterParameter("qq") long qq) {
        if (gmConfig.getIgnoreUsers().contains(qq)) {
            try {
                gmConfig.getIgnoreUsers().remove(qq);
                user.sendMessage("已解除对用户「{args.qq}」的屏蔽");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("取消屏蔽失败");
            }
        } else {
            user.sendMessage("用户「{args.qq}」不在屏蔽名单中");
        }
    }

    @Filter("(屏蔽名单|屏蔽列表|ignoreList)")
    @Required("essentials.group.ignore.list")
    public void listIgnore(GroupXiaomingUser user) {
        String list = "被屏蔽的用户有：\n";

        if (gmConfig.getIgnoreUsers().isEmpty()) {
            user.sendMessage("没有人要被屏蔽哦");
        } else {
            list = list.concat(CollectionUtil.toIndexString(gmConfig.getIgnoreUsers(),
                    xiaomingBot.getAccountManager()::getAliasAndCode));
            user.sendMessage(list);
        }
    }

    // 关键词撤回
    @Filter("(添加|add)(关键词|key) {r:关键词}")
    @Required("essentials.group.key.add")
    public void addGroupBannedEntry(GroupXiaomingUser user,
                                    @FilterParameter("关键词") String entry) {
        final Long group = user.getGroupCode();

        if (!MapUtil.getOrPutSupply(gmData.getGroupKeys(), group, HashSet::new).add(entry)) {
            user.sendMessage("本群已经有关键词「" + entry + "」需要撤回了哦");
        } else {
            user.sendMessage("成功添加需要撤回的关键词「" + entry + '」');
            xiaomingBot.getFileSaver().readyToSave(gmData);
        }
    }

    @Filter("(删除|remove)(关键词|key) {r:关键词}")
    @Required("essentials.group.key.remove")
    public void removeGroupBannedEntry(GroupXiaomingUser user,
                                       @FilterParameter("关键词") String entry) {
        final Long group = user.getGroupCode();

        if (gmData.getGroupKeys().containsKey(group)) {
            if (gmData.getGroupKeys().get(group).remove(entry)) {
                user.sendMessage("成功删除需要撤回的关键词「" + entry + '」');
                xiaomingBot.getFileSaver().readyToSave(gmData);
            } else {
                user.sendMessage("本群没有关键词「" + entry + "」要撤回哦");
            }
        } else {
            user.sendMessage("本群没有关键词「" + entry + "」要撤回哦");
        }
    }

    @Filter("(查看关键词|关键词列表|listKey)")
    @Required("essentials.group.key.list")
    public void listKey(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();
        String list = "本群要撤回的关键词有：\n";

        if (gmData.getGroupKeys().containsKey(group)) {
            if (gmData.getGroupKeys().get(group).isEmpty()) {
                user.sendMessage("本群没有要撤回的关键词哦");
            } else {
                list = list.concat(CollectionUtil.toIndexString(gmData.getGroupKeys().get(group)));
                user.sendMessage(list);
            }
        } else {
            user.sendMessage("本群没有要撤回的关键词哦");
        }
    }

    /*
        @Filter(value = "", pattern = FilterPattern.START_EQUAL)    // 使所有消息都发出 InteractEvent 事件
        public void recallKey(XiaomingUser user, Message message) {
        }
    */
    @Filter("(添加|创建|add)(迎新|迎新词|join) {r:迎新词}")
    @Required("essentials.group.join.add")
    public void addJoin(GroupXiaomingUser user,
                        @FilterParameter("迎新词") String entry) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            user.sendMessage("本群已经设置过迎新了哦");
        } else {
            gmData.getJoin().put(group, entry);
            user.sendMessage("成功添加入群欢迎词：\n" + entry);
            xiaomingBot.getFileSaver().readyToSave(gmData);
        }
    }

    @Filter("(修改|modify)(迎新|迎新词|join) {r:迎新词}")
    @Required("essentials.group.join.modify")
    public void modifyJoin(GroupXiaomingUser user,
                           @FilterParameter("迎新词") String entry) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            gmData.getJoin().put(group, entry);
            user.sendMessage("成功修改迎新词为：\n" + entry);
            xiaomingBot.getFileSaver().readyToSave(gmData);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    @Filter("(删除|remove)(迎新|迎新词|join)")
    @Required("essentials.group.join.remove")
    public void removeJoin(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            user.sendMessage("已移除迎新词：\n" + gmData.getJoin().get(group));
            gmData.getJoin().remove(group, gmData.getJoin().get(group));
            xiaomingBot.getFileSaver().readyToSave(gmData);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    @Filter("(查看迎新词|迎新词|listJoin)")
    @Required("essentials.group.join.list")
    public void listJoin(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();
        String list = "本群的迎新词为：\n";

        if (gmData.getJoin().containsKey(group) && !gmData.getJoin().get(group).isEmpty()) {
            list = list.concat(gmData.getJoin().get(group));
            user.sendMessage(list);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    // 防撤回
    @Filter("(启用|开启|enable)(防撤回|antiRecall)")
    @Required("essentials.group.antiRecall.enable")
    public void antiRecall(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiRecall().containsKey(group) && gmConfig.getAntiRecall().get(group)) {
            user.sendMessage("本群已经开启了防撤回了哦");
        } else {
            try {
                gmConfig.getAntiRecall().put(group, true);
                user.sendMessage("成功开启本群的防撤回功能");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("开启防撤回失败，可能是本群已开启防撤回");
            }
        }
    }

    @Filter("(关闭|disable)(防撤回|antiRecall)")
    @Required("essentials.group.antiRecall.disable")
    public void antiRecallDisabled(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiRecall().containsKey(group) && gmConfig.getAntiRecall().get(group)) {
            try {
                gmConfig.getAntiRecall().put(group, false);
                user.sendMessage("成功关闭本群的防撤回功能");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("关闭防撤回失败，可能是本群未开启防撤回");
            }
        } else
            user.sendMessage("本群尚未开启防撤回");
    }

    // 防闪照
    @Filter("(开启|启用|enable)(防闪照|antiFlash)")
    @Required("essentials.group.antiFlash.enable")
    public void antiFlash(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiFlash().containsKey(group) && gmConfig.getAntiFlash().get(group)) {
            user.sendMessage("本群已经开启了防闪照了哦");
        } else {
            try {
                gmConfig.getAntiFlash().put(group, true);
                user.sendMessage("成功开启本群的防闪照功能");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("开启防闪照失败，可能是本群已开启防闪照");
            }
        }
    }

    @Filter("(关闭|disable)(防闪照|antiFlash)")
    @Required("essentials.group.antiFlash.disable")
    public void antiFlashDisable(GroupXiaomingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiFlash().get(group) && gmConfig.getAntiFlash().containsKey(group)) {
            try {
                gmConfig.getAntiFlash().put(group, false);
                user.sendMessage("成功关闭本群的防闪照功能");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("关闭防闪照失败，可能是本群未开启防闪照");
            }
        } else {
            user.sendMessage("本群尚未开启防闪照");
        }
    }

    @Filter("反序列化 {r:r}")
    @Required("*")
    public void onFuck(XiaomingUser user, @FilterParameter("r") String miraiCode) {
        String deserialized = MiraiCode.deserializeMiraiCode(miraiCode).contentToString();

        if (deserialized.contains("flash"))
            return;

        user.sendMessage(deserialized);
    }

    // 自动审核开关
    @Filter("(开启|enable)(自动|auto)(审核|verify)")
    @Filter("enable auto verify")
    @Required("essentials.group.autoVerify.enable")
    public void enableAutoVerify(GroupXiaomingUser user) {
        final long group = user.getGroupCode();

        if (gmConfig.enableAutoVerify.containsKey(group) && gmConfig.enableAutoVerify.get(group)) {
            user.sendMessage("本群已经开启自动审核了哦");
        } else {
            try {
                gmConfig.enableAutoVerify.put(group, true);
                user.sendMessage("成功在本群开启加群自动审核");
                xiaomingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                user.sendMessage("开启失败，可能是本群已开启加群自动审核");
            }
        }
    }

    @Filter("(关闭|disable)(自动|auto)(审核|verify)")
    @Filter("disable auto verify")
    @Required("essentials.group.autoVerify.disable")
    public void disableAutoVerify(GroupXiaomingUser user) {
        final long group = user.getGroupCode();

        if (Boolean.TRUE.equals(gmConfig.getEnableAutoVerify().put(group, false))) {
            user.sendMessage("成功在本群关闭加群自动审核");
            xiaomingBot.getFileSaver().readyToSave(gmConfig);
        } else {
            user.sendMessage("关闭失败，可能是本群未开启加群自动审核");
        }
    }

    // 自动审核
    @Filter("(添加|add)(自动|auto)(审核|verify) {r:内容}")
    @Filter("add auto verify {r:内容}")
    @Required("essentials.group.autoVerify.add")
    public void addAutoVerify(GroupXiaomingUser user,
                              @FilterParameter("内容") String remain) {
        final long group = user.getGroupCode();
        Set<String> verify = MapUtil.getOrPutSupply(gmData.getAutoVerify(), group, HashSet::new);

        if (!gmConfig.getEnableAutoVerify().containsKey(group) || !gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        if (verify.contains(remain)) {
            user.sendMessage("本群的自动审核列表中已经有「" + remain + "」了哦");
        } else if (verify.add(remain)) {
            if (gmConfig.getAutoReject().get(group)) {
                user.sendMessage("成功为本群添加自动审核规则「" + remain + "」，当申请信息中包含「" + remain + "」时会自动通过审核（忽略大小写）"
                        + "\nPS：当未命中任何记录时将自动拒绝加群请求");
            } else {
                user.sendMessage("成功为本群添加自动审核规则「" + remain + "」，当申请信息中包含「" + remain + "」时会自动通过审核（忽略大小写）"
                        + "\nPS：回答错误时不会自动拒绝");
            }
            xiaomingBot.getFileSaver().readyToSave(gmData);
        }
    }

    @Filter("(删除|del|delete|remove)(自动|auto)(审核|verify) {r:内容/序号}")
    @Filter("(删除|del|delete|remove) auto verify {r:内容/序号}")
    @Required("essentials.group.autoVerify.remove")
    public void removeAutoVerify(GroupXiaomingUser user,
                                 @FilterParameter("内容/序号") String remain) {
        long group = user.getGroupCode();

        if (count == 0)
            tempIndex = CollectionUtil.copyOf(gmData.getAutoVerify().get(group));

        if (removeRemainOrIndex(gmData.getAutoVerify().get(group), group, remain)) {
            user.sendMessage("成功删除审核规则：\n" + reply);
            xiaomingBot.getFileSaver().readyToSave(gmData);
        } else {
            user.sendMessage("删除失败，可能是自动审核中没有该规则");
        }
    }

    @Filter("(查看|list)(自动|auto)(审核|verify)")
    @Filter("list auto content")
    @Required("essentials.group.autoVerify.list")
    public void listAutoVerify(GroupXiaomingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().containsKey(group) || !gmConfig.getEnableAutoVerify().get(group)) {
            count = 0;
            user.sendMessage("本群尚未开启自动审核");
            return;
        }

        if (gmData.autoVerify.get(group).isEmpty()) {
            count = 0;
            user.sendMessage("本群尚未添加审核规则");
            return;
        }

        try {
            if (gmConfig.autoReject.get(group)) {
                count = 0;
                user.sendMessage("当用户的申请信息中包含以下任意一条规则时加群请求会自动通过（需要管理员权限）：\n"
                        + CollectionUtil.toIndexString(gmData.getAutoVerify().get(group))
                        + "\nPS：当未命中任何规则时将自动拒绝加群请求");
            } else {
                count = 0;
                user.sendMessage("当用户的申请信息中包含以下任意一条规则时加群请求会自动通过（需要管理员权限）：\n"
                        + CollectionUtil.toIndexString(gmData.getAutoVerify().get(group))
                        + "\nPS：回答错误时不会自动拒绝");
            }
        } catch (Exception e) {
            user.sendMessage("查询时发生错误");
            e.printStackTrace();
        }
    }

    @Filter("(开启|enable)(自动|auto)(拒绝|reject)")
    @Filter("enable auto reject")
    @Required("essentials.group.enable.autoReject")
    public void enableAutoReject(GroupXiaomingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        if (gmConfig.getAutoReject().get(group)) {
            user.sendMessage("本群已经开启了审核不通过自动拒绝了哦");
        } else {
            gmConfig.getAutoReject().put(group, true);
            xiaomingBot.getFileSaver().readyToSave(gmConfig);
            user.sendMessage("成功在本群开启审核不通过自动拒绝");
        }
    }

    @Filter("(关闭|disable)(自动|auto)(拒绝|reject)")
    @Filter("disable auto reject")
    @Required("essentials.group.disable.autoReject")
    public void disableAutoReject(GroupXiaomingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        if (!gmConfig.getAutoReject().get(group)) {
            user.sendMessage("本群还未开启审核不通过自动拒绝哦");
        } else {
            gmConfig.getAutoReject().put(group, false);
            xiaomingBot.getFileSaver().readyToSave(gmConfig);
            user.sendMessage("成功在本群关闭审核不通过自动拒绝");
        }
    }
/*
    @Filter("图片 {r:图片链接}")
    public void image(XiaomingUser user, @FilterParameter("图片链接") String str) {
//        xiaomingBot.getScheduler().run(() -> {
//            try {
//                final URL url1 = new URL(MiraiCodeUtil.contentToString(str));
//                final Image image = user.uploadImage(ExternalResource.create(url1.openStream()));
//                user.sendMessage(image);
//            } catch (ConnectException connectException) {
//                connectException.printStackTrace();
//                user.sendMessage("连接超时");
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//                user.sendMessage("链接格式错误");
//            } catch (IOException e) {
//                e.printStackTrace();
//                user.sendMessage("未找到该图片");
//            }
//        });
        xiaomingBot.getScheduler().run(() -> {
            // generate url
            final URL url1;
            try {
                url1 = new URL(MiraiCodeUtil.contentToString(str));
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
                user.sendMessage("图片链接错误");
                return;
            }

            // upload image as external resource
            // and send
            try (final InputStream inputStream = url1.openStream();
                 final ExternalResource resource = ExternalResource.create(inputStream)) {
                user.sendMessage(user.uploadImage(resource));
            } catch (IOException exception) {
                user.sendError("请求失败：\n" + exception);
            }
        });
    }

    @Filter("查服 {r:ip}")
    public void server(XiaomingUser user, Message message, @FilterParameter("ip") String ip) {
        xiaomingBot.getScheduler().run(() -> {
            String[] address = new String[2];
            address[1] = "25565";
            URL url = null;

            if (ip.contains(":")) {
                address = ip.split(":", 2);
            } else if (ip.contains("：")) {
                address = ip.split("：", 2);
            }

            user.sendMessage("查询中，请稍后...");

            try {
                url = new URL(/*"https://api.imlazy.ink/mcapi/?name=欢迎使用xx查服        人数:" +
                        "&host=" + MiraiCodeUtil.contentToString(ip)
                        + "&type=image&getmotd=%0a%0a&getbg=3.jpg"*/
    /*
                        "https://api.onepage6.cn:3403/convenience/mc_server_state/?address=" + address[0] +
                                "&port=" + address[1]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                user.sendMessage("URL错误");
            }

            try (final InputStream inputStream = url.openStream()) {
                final Image image = user.uploadImage(ExternalResource.create(inputStream));
                user.sendMessage(image);
            } catch (Exception exception) {
                exception.printStackTrace();
                if (user instanceof GroupXiaomingUser) {
                    user.sendMessage(new At(user.getCode()).serializeToMiraiCode() + " 获取服务器信息失败：\n" + exception);
                } else {
                    user.sendMessage("获取服务器信息失败：\n" + exception);
                }
            }
        });
    }
    */
}
