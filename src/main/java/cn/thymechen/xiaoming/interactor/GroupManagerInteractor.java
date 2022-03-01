package cn.thymechen.xiaoming.interactor;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Maps;
import cn.chuanwise.xiaoming.util.MiraiCodes;
import cn.thymechen.xiaoming.EssentialsPlugin;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.thymechen.xiaoming.listener.GroupManagerListeners;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GroupManagerInteractor extends SimpleInteractors<EssentialsPlugin> {
    GroupManagerConfiguration gmConfig;
    GroupManagerData gmData;

    Set<String> tempIndex = new HashSet<>(); // 临时集合，记录索引信息
    int count = 0;  // 记录查询后的删除次数
    String reply;   // 回复

    private String removeFirstAndLast(@NotNull String str) {
        return (str.isEmpty()) ? str : str.substring(1, str.length() - 1);
    }

    private boolean removePatternAndString(Map<Long, Set<Pattern>> patternMap, Map<Long, Set<String>> stringMap, Long key, String remain) {
        if (patternMap.containsKey(key)) {
            for (Pattern pattern : patternMap.get(key)) {
                if (pattern.matcher(remain).find()) {
                    if (patternMap.get(key).remove(pattern)) {
                        stringMap.get(key).remove(pattern.pattern());
                        reply = pattern.pattern();
                        return true;
                    }
                }
                String ps = pattern.pattern();
                if (remain.equals(ps)) {
                    if (patternMap.get(key).remove(pattern)) {
                        stringMap.get(key).remove(remain);
                        reply = pattern.pattern();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean removeRemainOrIndex(Map<Long, Set<Pattern>> patternMap, Map<Long, Set<String>> stringMap, Long key, String str) {
        int index = -1;
        String remain = null;

        try {
            index = Integer.parseInt(str);
            remain = CollectionUtil.arrayGet(tempIndex, index - 1);
        } catch (Exception ignored) {

        }
        if (removePatternAndString(patternMap, stringMap, key, str)) {
            count++;
            return true;
        } else {
            if (index == -1 || remain == null)
                return false;

            if (removePatternAndString(GroupManagerListeners.verify, gmData.getAutoVerify(), key, remain)) {
                count++;
                return true;
            } else
                return false;
        }

    }

    @Override
    public void onRegister() {
        gmConfig = plugin.getGmConfig();
        gmData = plugin.getGmData();
    }

    @Filter("(设置|set)(默认|default)(禁言|mute)(时间|time) {time}")
    @Required("essentials.group.set.muteTime")
    public void setDefaultMuteTime(GroupXiaoMingUser user,
                                   @FilterParameter("time") long time) {
        final long group = user.getGroupCode();

        try {
            if (time <= TimeUnit.DAYS.toMillis(30) && time >= TimeUnit.MINUTES.toMillis(1)) {
                gmConfig.getDefaultMuteTime().put(group, (int) TimeUnit.MILLISECONDS.toMinutes(time));
                user.sendMessage("成功设置默认禁言时间为：" + gmConfig.getDefaultMuteTime().get(group) + "分钟");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            } else {
                user.sendMessage("「{arg.time}」不是一个有效值哦");
            }
        } catch (Exception e) {
            user.sendMessage("设置失败");
            e.printStackTrace();
        }
    }

    // 禁言
    @Filter("(禁言|mute) {qq}")
    @Required("essentials.group.mute")
    public void mute(GroupXiaoMingUser user,
                     @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        if (member.getUser().hasPermission("essentials.group.mute.bypass")) {
                            user.sendMessage("该用户不可被禁言");
                            return;
                        }
                        member.mute(TimeUnit.MINUTES.toMillis(gmConfig.getDefaultMuteTime().get(user.getContact().getCode())));
                        user.sendMessage("成功禁言「{arg.qq}」" + gmConfig.getDefaultMuteTime().get(user.getGroupCode()) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(禁言|mute) {qq} {r:time}")
    @Required("essentials.group.mute")
    public void mute(GroupXiaoMingUser user,
                     @FilterParameter("qq") long qq,
                     @FilterParameter("time") long time) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        if (member.getUser().hasPermission("essentials.group.mute.bypass")) {
                            user.sendMessage("该用户不可被禁言");
                            return;
                        }
                        member.mute(time);
                        user.sendMessage("成功禁言「{arg.qq}」" + TimeUnit.MILLISECONDS.toMinutes(time) + "分钟");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(永久|forever)(禁言|mute) {qq}")
    @Required("essentials.group.mute")
    public void muteForever(GroupXiaoMingUser user, @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        if (member.getUser().hasPermission("essentials.group.mute.bypass")) {
                            user.sendMessage("该用户不可被禁言");
                            return;
                        }
                        List<Long> mute = Maps.getOrPutSupply(gmConfig.getMuteForever(), user.getGroupCode(), ArrayList::new);
                        if (!mute.contains(qq)) {
                            mute.add(qq);
                            member.mute(TimeUnit.DAYS.toMillis(30));
                            xiaoMingBot.getFileSaver().readyToSave(gmConfig);
                            user.sendMessage("成功永久禁言「{arg.qq}」");
                        } else
                            user.sendMessage("「{arg.qq}」已被永久禁言");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(取消|解除|cancel|un)(永久|forever)(禁言|mute) {qq}")
    @Required("essentials.group.unmute")
    public void cancelMuteForever(GroupXiaoMingUser user, @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        List<Long> mute = Maps.getOrPutSupply(gmConfig.getMuteForever(), user.getGroupCode(), ArrayList::new);
                        if (mute.contains(qq)) {
                            mute.remove(qq);
                            member.unmute();
                            xiaoMingBot.getFileSaver().readyToSave(gmConfig);
                            user.sendMessage("成功取消「{arg.qq}」的永久禁言");
                        } else
                            user.sendMessage("「{arg.qq}」未被永久禁言");
                    } catch (Exception exception) {
                        user.sendMessage("禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    @Filter("(解除禁言|unmute) {qq}")
    @Required("essentials.group.unmute")
    public void unmute(GroupXiaoMingUser user,
                       @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    long group = user.getGroupCode();
                    try {
                        if (Maps.getOrPutSupply(gmConfig.getMuteForever(), group, ArrayList::new).contains(qq)) {
                            user.sendMessage("该用户已被永久禁言，不可解除禁言");
                            return;
                        }
                        member.unmute();
                        user.sendMessage("成功解除「{arg.qq}」的禁言");
                    } catch (Exception exception) {
                        user.sendMessage("解除禁言时出现错误，可能是没有权限");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    // 踢人
    @Filter("(踢|踢出|kick) {qq}")
    @Required("essentials.group.kick")
    public void kick(GroupXiaoMingUser user,
                     @FilterParameter("qq") long qq) {
        user.getContact().getMember(qq)
                .ifPresentOrElse(member -> {
                    try {
                        if (member.getUser().hasPermission("essentials.group.kick.bypass")) {
                            user.sendMessage("该用户不可被移出群聊");
                            return;
                        }
                        member.kick("");
                        user.sendMessage("已将「{arg.qq}」移出本群");
                    } catch (Exception exception) {
                        user.sendMessage("移出失败");
                    }
                }, () -> user.sendError("「{arg.qq}」不在本群"));
    }

    // 屏蔽用户
    @Filter("(屏蔽|ignore)(用户|user) {qq}")
    @Filter("(屏蔽|ignore)(用户|user) {qq} ")
    @Required("essentials.group.ignore.add.user")
    public void ignoreGroups(XiaoMingUser user,
                             @FilterParameter("qq") long qq) {
        if (!gmConfig.getIgnoreUsers().contains(qq)) {
            try {
                gmConfig.getIgnoreUsers().add(qq);

                user.sendMessage("成功屏蔽用户「{args.qq}」");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
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
    public void unIgnore(XiaoMingUser user,
                         @FilterParameter("qq") long qq) {
        if (gmConfig.getIgnoreUsers().contains(qq)) {
            try {
                gmConfig.getIgnoreUsers().remove(qq);
                user.sendMessage("已解除对用户「{args.qq}」的屏蔽");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
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
    public void listIgnore(GroupXiaoMingUser user) {
        String list = "被屏蔽的用户有：\n";

        if (gmConfig.getIgnoreUsers().isEmpty()) {
            user.sendMessage("没有人要被屏蔽哦");
        } else {
            list = list.concat(CollectionUtil.toIndexString(gmConfig.getIgnoreUsers(),
                    xiaoMingBot.getAccountManager()::getAliasAndCode));
            user.sendMessage(list);
        }
    }

    // 关键词撤回
    @Filter("(添加|add)(关键词|key) {r:关键词}")
    @Required("essentials.group.key.add")
    public void addGroupBannedEntry(GroupXiaoMingUser user,
                                    @FilterParameter("关键词") String entry) {
        final Long group = user.getGroupCode();
        entry = MiraiCodes.contentToString(entry);
        Set<String> key = Maps.getOrPutSupply(gmData.getGroupKeys(), group, HashSet::new);

        if (!key.add(entry)) {
            user.sendMessage("本群已经有关键词「" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(entry)) + "」需要撤回了哦");
        } else {
            try {
                Maps.getOrPutSupply(GroupManagerListeners.keys, group, HashSet::new).add(Pattern.compile(entry, Pattern.CASE_INSENSITIVE));
                xiaoMingBot.getFileSaver().readyToSave(gmData);
                user.sendMessage("成功添加需要撤回的关键词「" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(entry)) + "」");
            } catch (PatternSyntaxException exception) {
                user.sendMessage("正则表达式编译错误：\n" + xiaoMingBot.getSerializer().serialize(exception.getMessage()).replaceAll("\"", ""));
                key.remove(entry);
            }
        }
    }

    @Filter("(删除|remove)(关键词|key) {r:关键词}")
    @Required("essentials.group.key.remove")
    public void removeGroupBannedEntry(GroupXiaoMingUser user,
                                       @FilterParameter("关键词") String entry) {
        final Long group = user.getGroupCode();
        entry = MiraiCodes.contentToString(entry);

        if (GroupManagerListeners.keys.containsKey(group))
            if (removePatternAndString(GroupManagerListeners.keys, gmData.getGroupKeys(), group, entry)) {
                xiaoMingBot.getFileSaver().readyToSave(gmData);
                user.sendMessage("成功删除需要撤回的关键词「" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(reply)) + '」');
                return;
            }
        user.sendMessage("本群没有关键词「" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(entry)) + "」要撤回哦");
    }

    @Filter("(查看关键词|关键词列表|listKey)")
    @Required("essentials.group.key.list")
    public void listKey(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmData.getGroupKeys().containsKey(group)) {
            if (gmData.getGroupKeys().get(group).isEmpty() || Objects.isNull(gmData.getGroupKeys().get(group))) {
                user.sendMessage("本群没有要撤回的关键词哦");
            } else {
                String list = "本群要撤回的关键词有：\n" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(CollectionUtil.toIndexString(gmData.getGroupKeys().get(group))));
                user.sendMessage(list);
            }
        } else {
            user.sendMessage("本群没有要撤回的关键词哦");
        }
    }

    @Filter("(添加|创建|add)(迎新|迎新词|欢迎|欢迎词|join) {r:迎新词}")
    @Required("essentials.group.join.add")
    public void addJoin(GroupXiaoMingUser user,
                        @FilterParameter("迎新词") String entry) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            user.sendMessage("本群已经设置过迎新了哦");
        } else {
            gmData.getJoin().put(group, entry);
            user.sendMessage("成功添加入群欢迎词：\n" + entry);
            xiaoMingBot.getFileSaver().readyToSave(gmData);
        }
    }

    @Filter("(修改|modify)(迎新|迎新词|欢迎|欢迎词|join) {r:迎新词}")
    @Required("essentials.group.join.modify")
    public void modifyJoin(GroupXiaoMingUser user,
                           @FilterParameter("迎新词") String entry) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            gmData.getJoin().put(group, entry);
            user.sendMessage("成功修改迎新词为：\n" + entry);
            xiaoMingBot.getFileSaver().readyToSave(gmData);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    @Filter("(删除|remove)(迎新|迎新词|欢迎|欢迎词|join)")
    @Required("essentials.group.join.remove")
    public void removeJoin(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmData.getJoin().containsKey(group)) {
            user.sendMessage("已移除迎新词：\n" + gmData.getJoin().get(group));
            gmData.getJoin().remove(group, gmData.getJoin().get(group));
            xiaoMingBot.getFileSaver().readyToSave(gmData);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    @Filter("(查看迎新词|迎新词|listJoin)")
    @Required("essentials.group.join.list")
    public void listJoin(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();
        String list = "本群的迎新词为：\n";

        if (gmData.getJoin().containsKey(group) && !gmData.getJoin().get(group).isEmpty() && !Objects.isNull(gmData.getJoin().get(group))) {
            list = list.concat(gmData.getJoin().get(group));
            user.sendMessage(list);
        } else {
            user.sendMessage("本群还没有设置入群欢迎呢，使用「添加迎新 {欢迎词}」创建一个吧");
        }
    }

    // 防撤回
    @Filter("(启用|开启|enable)(防撤回|antiRecall)")
    @Required("essentials.group.antiRecall.enable")
    public void antiRecall(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiRecall().containsKey(group) && gmConfig.getAntiRecall().get(group)) {
            user.sendMessage("本群已经开启了防撤回了哦");
        } else {
            try {
                gmConfig.getAntiRecall().put(group, true);
                user.sendMessage("成功开启本群的防撤回功能");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("开启防撤回失败，可能是本群已开启防撤回");
            }
        }
    }

    @Filter("(关闭|disable)(防撤回|antiRecall)")
    @Required("essentials.group.antiRecall.disable")
    public void antiRecallDisabled(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiRecall().containsKey(group) && gmConfig.getAntiRecall().get(group)) {
            try {
                gmConfig.getAntiRecall().put(group, false);
                user.sendMessage("成功关闭本群的防撤回功能");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
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
    public void antiFlash(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiFlash().containsKey(group) && gmConfig.getAntiFlash().get(group)) {
            user.sendMessage("本群已经开启了防闪照了哦");
        } else {
            try {
                gmConfig.getAntiFlash().put(group, true);
                user.sendMessage("成功开启本群的防闪照功能");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("开启防闪照失败，可能是本群已开启防闪照");
            }
        }
    }

    @Filter("(关闭|disable)(防闪照|antiFlash)")
    @Required("essentials.group.antiFlash.disable")
    public void antiFlashDisable(GroupXiaoMingUser user) {
        final Long group = user.getGroupCode();

        if (gmConfig.getAntiFlash().get(group) && gmConfig.getAntiFlash().containsKey(group)) {
            try {
                gmConfig.getAntiFlash().put(group, false);
                user.sendMessage("成功关闭本群的防闪照功能");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage("关闭防闪照失败，可能是本群未开启防闪照");
            }
        } else {
            user.sendMessage("本群尚未开启防闪照");
        }
    }

    @Filter("反序列化 {r:r}")
    public void Deserialized(XiaoMingUser user, @FilterParameter("r") String str) {
        String deserialized = MiraiCode.deserializeMiraiCode(str).contentToString();
        user.sendMessage(deserialized);
    }

    @Filter("序列化 {r:r}")
    public void Serialized(XiaoMingUser user, @FilterParameter("r") String str) {
        MessageChain serialized = MiraiCodes.asMessageChain(str);
        user.sendMessage(serialized);
    }

    // 自动审核开关
    @Filter("(开启|enable)(自动|auto)(审核|verify)")
    @Filter("enable auto verify")
    @Required("essentials.group.autoVerify.enable")
    public void enableAutoVerify(GroupXiaoMingUser user) {
        final long group = user.getGroupCode();

        if (gmConfig.enableAutoVerify.containsKey(group) && gmConfig.enableAutoVerify.get(group)) {
            user.sendMessage("本群已经开启自动审核了哦");
        } else {
            try {
                gmConfig.enableAutoVerify.put(group, true);
                user.sendMessage("成功在本群开启加群自动审核");
                xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            } catch (Exception e) {
                user.sendMessage("开启失败，可能是本群已开启加群自动审核");
            }
        }
    }

    @Filter("(关闭|disable)(自动|auto)(审核|verify)")
    @Filter("disable auto verify")
    @Required("essentials.group.autoVerify.disable")
    public void disableAutoVerify(GroupXiaoMingUser user) {
        final long group = user.getGroupCode();

        if (Boolean.TRUE.equals(gmConfig.getEnableAutoVerify().put(group, false))) {
            user.sendMessage("成功在本群关闭加群自动审核");
            xiaoMingBot.getFileSaver().readyToSave(gmConfig);
        } else {
            user.sendMessage("关闭失败，可能是本群未开启加群自动审核");
        }
    }

    // 自动审核
    @Filter("(添加|add)(自动|auto)(审核|verify) {r:内容}")
    @Filter("add auto verify {r:内容}")
    @Required("essentials.group.autoVerify.add")
    public void addAutoVerify(GroupXiaoMingUser user,
                              @FilterParameter("内容") String remain) {
        final long group = user.getGroupCode();
        Set<String> verify = Maps.getOrPutSupply(gmData.getAutoVerify(), group, HashSet::new);

        if (!gmConfig.getEnableAutoVerify().containsKey(group) || !gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        remain = MiraiCodes.contentToString(remain);
        if (verify.contains(remain)) {
            user.sendMessage("本群的自动审核列表中已经有「" + remain + "」了哦");
        } else if (verify.add(remain)) {
            if (gmConfig.getAutoReject().get(group)) {
                try {
                    Maps.getOrPutSupply(GroupManagerListeners.verify, group, HashSet::new).add(Pattern.compile(MiraiCodes.contentToString(remain), Pattern.CASE_INSENSITIVE));
                    xiaoMingBot.getFileSaver().readyToSave(gmData);
                    user.sendMessage("成功为本群添加自动审核规则「" + remain + "」，当申请信息中包含「" + remain + "」时会自动通过审核（忽略大小写）"
                            + "\nPS：当未命中任何记录时将自动拒绝加群请求");
                } catch (PatternSyntaxException exception) {
                    user.sendMessage("正则表达式编译错误：\n" + xiaoMingBot.getSerializer().serialize(exception.getMessage()).replaceAll("\"", ""));
                    verify.remove(MiraiCodes.contentToString(remain));
                }
            } else {
                try {
                    Maps.getOrPutSupply(GroupManagerListeners.verify, group, HashSet::new).add(Pattern.compile(MiraiCodes.contentToString(remain), Pattern.CASE_INSENSITIVE));
                    xiaoMingBot.getFileSaver().readyToSave(gmData);
                    user.sendMessage("成功为本群添加自动审核规则「" + remain + "」，当申请信息中包含「" + remain + "」时会自动通过审核（忽略大小写）"
                            + "\nPS：回答错误时不会自动拒绝");
                } catch (PatternSyntaxException exception) {
                    user.sendMessage("正则表达式编译错误：\n" + xiaoMingBot.getSerializer().serialize(exception.getMessage()).replaceAll("\"", ""));
                    verify.remove(MiraiCodes.contentToString(remain));
                }
            }
            count = 0;
        }
    }

    @Filter("(删除|del|delete|remove)(自动|auto)(审核|verify) {r:内容/序号}")
    @Filter("(删除|del|delete|remove) auto verify {r:内容/序号}")
    @Required("essentials.group.autoVerify.remove")
    public void removeAutoVerify(GroupXiaoMingUser user,
                                 @FilterParameter("内容/序号") String remain) {
        long group = user.getGroupCode();
        remain = MiraiCodes.contentToString(remain);

        if (count == 0)
            tempIndex = CollectionUtil.copyOf(gmData.getAutoVerify().get(group));

        if (removeRemainOrIndex(GroupManagerListeners.verify, gmData.getAutoVerify(), group, remain)) {
            xiaoMingBot.getFileSaver().readyToSave(gmData);
            user.sendMessage("成功删除审核规则：\n" + removeFirstAndLast(xiaoMingBot.getSerializer().serialize(reply)));
        } else {
            user.sendMessage("删除失败，可能是自动审核中没有该规则");
        }
    }

    @Filter("(查看|list)(自动|auto)(审核|verify)")
    @Filter("list auto content")
    @Required("essentials.group.autoVerify.list")
    public void listAutoVerify(GroupXiaoMingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().containsKey(group) || !gmConfig.getEnableAutoVerify().get(group)) {
            count = 0;
            user.sendMessage("本群尚未开启自动审核");
            return;
        }

        if (!gmData.getAutoVerify().containsKey(group) || gmData.getAutoVerify().get(group).isEmpty() || Objects.isNull(gmData.getAutoVerify().get(group))) {
            count = 0;
            user.sendMessage("本群尚未添加审核规则");
            return;
        }

        try {
            if (gmConfig.getAutoReject().get(group)) {
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
    public void enableAutoReject(GroupXiaoMingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        if (gmConfig.getAutoReject().get(group)) {
            user.sendMessage("本群已经开启了审核不通过后自动拒绝了哦");
        } else {
            gmConfig.getAutoReject().put(group, true);
            xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            user.sendMessage("成功在本群开启审核不通过后自动拒绝");
        }
    }

    @Filter("(关闭|disable)(自动|auto)(拒绝|reject)")
    @Filter("disable auto reject")
    @Required("essentials.group.disable.autoReject")
    public void disableAutoReject(GroupXiaoMingUser user) {
        final long group = user.getGroupCode();

        if (!gmConfig.getEnableAutoVerify().get(group)) {
            user.sendMessage("本群还没有开启自动审核哦");
            return;
        }

        if (!gmConfig.getAutoReject().get(group)) {
            user.sendMessage("本群还未开启审核不通过自动拒绝哦");
        } else {
            gmConfig.getAutoReject().put(group, false);
            xiaoMingBot.getFileSaver().readyToSave(gmConfig);
            user.sendMessage("成功在本群关闭审核不通过自动拒绝");
        }
    }
/*
    @Filter("图片 {r:图片链接}")
    public void image(XiaoMingUser user, @FilterParameter("图片链接") String str) {
        xiaoMingBot.getScheduler().run(() -> {
            try {
                final URL url1 = new URL(MiraiCodes.contentToString(str));
                final Image image = user.uploadImage(ExternalResource.create(url1.openStream()));
                user.sendMessage(image);
            } catch (ConnectException connectException) {
                connectException.printStackTrace();
                user.sendMessage("连接超时");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                user.sendMessage("链接格式错误");
            } catch (IOException e) {
                e.printStackTrace();
                user.sendMessage("未找到该图片");
            }
        });
        xiaoMingBot.getScheduler().run(() -> {
            // generate url
            final URL url1;
            try {
                url1 = new URL(MiraiCodes.contentToString(str));
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
    */
}
