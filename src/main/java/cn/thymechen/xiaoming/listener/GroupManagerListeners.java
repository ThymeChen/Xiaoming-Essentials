package cn.thymechen.xiaoming.listener;

import cn.chuanwise.toolkit.sized.SizedCopyOnWriteArrayList;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Maps;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.contact.contact.MemberContact;
import cn.chuanwise.xiaoming.user.MemberXiaoMingUser;
import cn.thymechen.xiaoming.EssentialsPlugin;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.listener.ListenerPriority;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.PrivateXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import cn.thymechen.xiaoming.util.VerifyImage;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.FlashImage;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GroupManagerListeners extends SimpleListeners<EssentialsPlugin> {
    public static Map<Long, Set<Pattern>> keys = new ConcurrentHashMap<>();     // 关键词的正则表达式
    public static Map<Long, Set<Pattern>> verify = new ConcurrentHashMap<>();   // 加群审核
    public static List<MessageEvent> messageEvents;     // 最近消息缓存

    @Override
    public void onRegister() {
        messageEvents = new SizedCopyOnWriteArrayList<>(plugin.getCoreConfig().getMaxMessagesCache());

        GroupManagerData groupManagerData = plugin.getGmData();
        // 编译关键词
        for (long group : groupManagerData.getGroupKeys().keySet()) {
            for (String str : groupManagerData.getGroupKeys().get(group)) {
                try {
                    Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
                    Maps.getOrPutSupply(keys, group, HashSet::new).add(pattern);
                } catch (PatternSyntaxException exception) {
                    getLogger().error("正则表达式编译错误：", exception);
                    getLogger().error("请删除该表达式或将其修改为正确的格式后重新载入本插件");
                    xiaoMingBot.getScheduler().runLater(5000, () -> xiaoMingBot.getPluginManager().disablePlugin(plugin));
                }
            }
        }
        // 编译自动审核
        for (long group : groupManagerData.getAutoVerify().keySet()) {
            for (String str : groupManagerData.getAutoVerify().get(group)) {
                try {
                    Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
                    Maps.getOrPutSupply(verify, group, HashSet::new).add(pattern);
                } catch (PatternSyntaxException exception) {
                    getLogger().error("正则表达式编译错误：", exception);
                    getLogger().error("请删除该表达式或将其修改为正确的格式后重新载入本插件");
                    xiaoMingBot.getScheduler().runLater(5000, () -> xiaoMingBot.getPluginManager().disablePlugin(plugin));
                }
            }
        }
    }

    // 将消息加进缓存
    @EventListener(listenCancelledEvent = true)
    public void addMessageEvents(MessageEvent event) {
        messageEvents.add(event);
    }

    // bot加入新群
    @EventListener
    public void joinNewGroup(@NotNull BotJoinGroupEvent botJoinGroupEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final long group = botJoinGroupEvent.getGroupId();

        gmConfiguration.getDefaultMuteTime().put(group, 10);
        gmConfiguration.getAutoReject().put(group, false);
        gmConfiguration.getVerifyConfig().getJoinVerify().put(group, false);

        xiaoMingBot.getFileSaver().readyToSave(gmConfiguration);
    }

    // bot退出群聊
    @EventListener
    public void botLeaveGroup(@NotNull BotLeaveEvent event) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final long group = event.getGroupId();

        gmConfiguration.getDefaultMuteTime().remove(group);
        gmConfiguration.getAutoReject().remove(group);
        gmConfiguration.getVerifyConfig().getJoinVerify().remove(group);

        xiaoMingBot.getFileSaver().readyToSave(gmConfiguration);
    }

    // 屏蔽（取消 MessageEvent）
    @EventListener(priority = ListenerPriority.HIGH)
    public void matchIgnoreUsers(@NotNull MessageEvent messageEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final long qq = messageEvent.getUser().getCode();

        if (gmConfiguration.ignoreUsers.contains(qq)) {
            messageEvent.cancel();
        }
    }

    // 关键词撤回（最高优先级，不受 ignoreUsers 影响）
    @EventListener(priority = ListenerPriority.HIGHEST)
    public void recallKey(@NotNull MessageEvent messageEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();

        if (!(messageEvent.getUser() instanceof GroupXiaoMingUser) || messageEvent.getUser().hasPermission("essentials.group.bypass.mute"))
            return;

        GroupXiaoMingUser user = (GroupXiaoMingUser) messageEvent.getUser();
        long group = user.getGroupCode();

        Message message = messageEvent.getMessage();
        String mes = message.serialize().toLowerCase(Locale.ROOT);

        final int botPermLevel = Objects.requireNonNull(xiaoMingBot.getMiraiBot().getGroup(((GroupXiaoMingUser) messageEvent.getUser()).getGroupCode())).getBotPermission().getLevel();
        final int memberPermLevel = user.getMemberContact().getPermission().getLevel();

        if (keys.containsKey(group)) {
            if (mes.contains("删除关键词") || mes.contains("添加关键词") || mes.contains("查看关键词"))
                return;

            if (botPermLevel <= memberPermLevel)
                return;

            for (Pattern key : keys.get(group)) {
                if (key.matcher(mes).find())
                    try {
                        message.recall();
                        messageEvent.cancel();
                        user.mute(gmConfiguration.defaultMuteTime.get(group) * 60000);
                        xiaoMingBot.getContactManager().sendGroupMessage(group,
                                new At(user.getCode()).serializeToMiraiCode() + " 你发送了一条违规消息");
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        xiaoMingBot.getContactManager().sendGroupMessage(group, "撤回关键词失败：\n" + e);
                        return;
                    }
            }
        }
    }

    // 迎新
    @EventListener
    public void join(@NotNull MemberJoinEvent joinEvent) {
        final GroupManagerData gmData = plugin.getGmData();

        final Long group = joinEvent.getGroupId();
        final long qq = joinEvent.getMember().getId();

        if (gmData.join.containsKey(group)) {
            if (gmData.join.get(group) != null)
                xiaoMingBot.getContactManager().sendGroupMessage(group,
                        new At(qq).serializeToMiraiCode() + ' ' + gmData.getJoin().get(group));
        }
    }

    // 防撤回
    @EventListener(priority = ListenerPriority.LOW, listenCancelledEvent = true)
    public void antiRecall(@NotNull MessageRecallEvent.GroupRecall recall) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();

        final long groupCode = recall.getGroup().getId();   // 群号
        final long authorCode = recall.getAuthorId();   // 消息原作者
        final long operatorCode = Objects.requireNonNull(recall.getOperator()).getId(); // 撤回事件的操作者

        // 判断是否被屏蔽 及 操作者是否为机器人
        if (gmConfiguration.getIgnoreUsers().contains(authorCode) || operatorCode == xiaoMingBot.getCode())
            return;

        // 被撤回消息的id
        final int[] messageId = recall.getMessageIds();

        if (!gmConfiguration.getAntiRecall().containsKey(groupCode)
                || !gmConfiguration.getAntiRecall().get(groupCode)
                || gmConfiguration.getIgnoreUsers().contains(authorCode))
            return;

        // 获得被撤回的人最近发送的消息缓存
        final MessageEvent messageEvent = CollectionUtil.first(messageEvents, event -> {
            final XiaoMingUser user = event.getUser();
            return user instanceof GroupXiaoMingUser &&
                    ((GroupXiaoMingUser) user).getGroupCode() == groupCode &&
                    Arrays.equals(messageId, event.getMessage().getMessageCode());
        });

        if (Objects.isNull(messageEvent)) {
            // message not found
            xiaoMingBot.getContactManager().sendGroupMessage(groupCode, new At(operatorCode).serializeToMiraiCode()
                    + " 刚刚撤回了 " + new At(authorCode).serializeToMiraiCode() + " 的消息，但时间找不到");
        } else {
            final Message message = messageEvent.getMessage();

            xiaoMingBot.getContactManager().sendGroupMessage(groupCode, new At(operatorCode).serializeToMiraiCode()
                    + " 刚刚撤回了 " + new At(authorCode).serializeToMiraiCode() + " 的消息：\n" + message.serialize());
        }
    }

    // 防闪照
    @EventListener
    public void antiFlash(@NotNull GroupMessageEvent groupMessageEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final long group = groupMessageEvent.getGroup().getId();
        final long qq = groupMessageEvent.getSender().getId();
        final MessageChain messages = groupMessageEvent.getMessage();

        FlashImage flashImage = (FlashImage) messages.stream()
                .filter(FlashImage.class::isInstance)
                .findFirst()
                .orElse(null);

        // 判断消息是否包含闪照 及 发送消息者是否被屏蔽
        if (flashImage == null || gmConfiguration.getIgnoreUsers().contains(qq))
            return;

        if (!gmConfiguration.getAntiFlash().containsKey(group) || !gmConfiguration.getAntiFlash().get(group))
            return;

        xiaoMingBot.getContactManager().sendGroupMessage(group, new At(qq).serializeToMiraiCode() +
                " 发送了一张闪照，原图为：\n" + flashImage.getImage());

        xiaoMingBot.getContactManager().getGroupContact(group).get().sendMessage(new PlainText(flashImage.toString()));
    }

    // 根据 miraiCode 发送闪照原图（仅限私聊）
    @EventListener
    public void flash(@NotNull MessageEvent messageEvent) {
        GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        XiaoMingUser user = messageEvent.getUser();

        // 判断是否为私聊消息
        if (!(user instanceof PrivateXiaoMingUser))
            return;

        // 判断是否被屏蔽
        if (gmConfiguration.getIgnoreUsers().contains(user.getCode()))
            return;

        String message = MiraiCode.deserializeMiraiCode(messageEvent.getMessage().serialize()).contentToString();
        if (message.startsWith("[") && message.endsWith("]"))
            if (message.contains("flash")) {
                message = message.replace("flash", "image");
                user.sendMessage(message);
            }
        StringBuffer stringBuffer = new StringBuffer();
    }

    // 加群自动审核
    @EventListener
    public void autoVerify(@NotNull MemberJoinRequestEvent requestEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final GroupManagerData gmData = plugin.getGmData();

        final long group = requestEvent.getGroupId();
        XiaoMingUser owner = xiaoMingBot.getContactManager().getGroupContact(group).get().getOwner().getUser();
        String request = requestEvent.getMessage();

        if (!gmConfiguration.getEnableAutoVerify().containsKey(group))
            return;

        if (!gmConfiguration.getEnableAutoVerify().get(group)
                || !verify.containsKey(group)
                || verify.get(group).isEmpty()
                || Objects.isNull(verify.get(group))) {
            owner.sendMessage("「" + requestEvent.getFromId()
                    + "(" + requestEvent.getFromNick() + ")」向你所管理的群聊「" + group + "("
                    + xiaoMingBot.getContactManager().getGroupContact(group).get().getAlias() + "）」发送了一条加群请求，其具体内容为：\n"
                    + request);
            return;
        }

        for (Pattern key : verify.get(group)) {
            if (key.matcher(request).find()) {
                try {
                    requestEvent.accept();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    xiaoMingBot.getContactManager().sendGroupMessage(group, "加群自动审核时出现错误，请到后台查看报错");
                    return;
                }
            }
        }

        if (gmConfiguration.getAutoReject().get(group)) {
            requestEvent.reject(false, "回答错误，不予通过");
        } else {
            owner.sendMessage("「" + requestEvent.getFromId() + "("
                    + requestEvent.getFromNick() + ")」向你所管理的群聊「" + group + "("
                    + xiaoMingBot.getContactManager().getGroupContact(group).get().getAlias() + "）」发送了一条加群请求，其具体内容为：\n"
                    + request);
        }
    }

    // 永久禁言
    @EventListener
    public void mute(@NotNull MemberUnmuteEvent event) {
        GroupManagerConfiguration gmConfig = plugin.getGmConfig();
        long group = event.getGroupId();
        Optional<GroupContact> groupContact = xiaoMingBot.getContactManager().getGroupContact(group);
        Map<Long, List<Long>> mute = new ConcurrentHashMap<>();

        groupContact.flatMap(contact -> contact.getMember(event.getMember().getId()))
                .ifPresent(member -> {
                    if (Maps.getOrPutSupply(mute, group, ArrayList::new).contains(member.getCode()))
                        member.mute(TimeUnit.DAYS.toMillis(30));
                });
    }

    // 入群验证
    @EventListener
    public void memberJoinVerify(@NotNull MemberJoinEvent event) {
        GroupManagerConfiguration gmConfig = plugin.getGmConfig();
        if (!gmConfig.getVerifyConfig().getJoinVerify().get(event.getGroupId()))
            return;

        xiaoMingBot.getScheduler().run(() -> {
            long groupId = event.getGroupId();
            long qq = event.getMember().getId();
            MemberContact member = xiaoMingBot.getContactManager().getGroupContact(groupId).get().getMember(qq).get();

            char[] verify = new char[gmConfig.getVerifyConfig().getVerifyLength()];
            for (int i = 0; i < gmConfig.getVerifyConfig().getVerifyLength(); i++)
                verify[i] = gmConfig.getVerifyConfig().getVerifyCode().charAt(new Random().nextInt(gmConfig.getVerifyConfig().getVerifyCode().length()));
            String verifyCode = String.valueOf(verify);

            MemberXiaoMingUser memberUser = member.getUser();
            GroupXiaoMingUser user = xiaoMingBot.getReceptionistManager().getReceptionist(qq).getGroupXiaoMingUser(groupId).get();

            BufferedImage image = VerifyImage.getImage(verify);
            try (
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream)
            ) {
                ImageIO.write(image, "png", imageOutputStream);

                try (
                        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                        ExternalResource externalResource = ExternalResource.create(inputStream)
                ) {
                    xiaoMingBot.getContactManager().sendGroupMessage(groupId,
                            new At(qq).serializeToMiraiCode()
                                    + " 欢迎加入本群，为保障良好的聊天环境，请在180秒内输入以下验证码。验证码不区分大小写\n"
                                    + member.uploadImage(externalResource).serializeToMiraiCode());
                }

                user.nextMessage(180 * 1000)
                        .ifPresentOrElse(messageEvent -> {
                            if (messageEvent.getMessageChain().serializeToMiraiCode().equalsIgnoreCase(verifyCode))
                                member.sendMessage("验证成功！现在你可以愉快地聊天了！");
                            else
                                member.kick("验证码错误");
                        }, () -> member.kick("验证超时"));

//                xiaoMingBot.getContactManager().nextGroupMemberMessage(groupId, qq, 180 * 1000)
//                        .ifPresentOrElse(messageEvent -> {
//                            if (messageEvent.getMessage().serialize().equalsIgnoreCase(verifyCode))
//                                member.sendMessage("验证成功！现在你可以愉快地聊天了！");
//                            else
//                                member.kick("验证码错误");
//                        }, () -> member.kick("验证超时"));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
