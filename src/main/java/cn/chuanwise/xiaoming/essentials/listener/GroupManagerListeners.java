package cn.chuanwise.xiaoming.essentials.listener;

import cn.chuanwise.toolkit.sized.SizedCopyOnWriteArrayList;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.MapUtil;
import cn.chuanwise.xiaoming.essentials.EssentialsPlugin;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.InteractEvent;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.listener.ListenerPriority;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.PrivateXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;

import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.FlashImage;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class GroupManagerListeners extends SimpleListeners<EssentialsPlugin> {
    public static Map<Long, Set<Pattern>> keys = new HashMap<>(); // 关键词的正则表达式
    public List<MessageEvent> messageEvents;    // 最近消息缓存

    static {
        GroupManagerData groupManagerData = EssentialsPlugin.INSTANCE.getGmData();
        for (long group : groupManagerData.getGroupKeys().keySet()) {
            for (String str : groupManagerData.getGroupKeys().get(group)) {
                Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
                MapUtil.getOrPutSupply(keys, group, HashSet::new).add(pattern);
            }
        }
    }

    @Override
    public void onRegister() {
        messageEvents = new SizedCopyOnWriteArrayList<>(plugin.getCoreConfig().getMessagesCache());
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

        xiaomingBot.getFileSaver().readyToSave(gmConfiguration);
    }

    // 屏蔽（取消交互事件）
    @EventListener(priority = ListenerPriority.HIGH)
    public void matchIgnoreUsers(@NotNull InteractEvent interactEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final long qq = interactEvent.getContext().getUser().getCode();

        if (gmConfiguration.ignoreUsers.contains(qq)) {
            interactEvent.cancel();
        }
    }

    // 关键词撤回（最高优先级，不受 ignoreUsers 影响）
    @EventListener(priority = ListenerPriority.HIGHEST)
    public void recallKey(@NotNull MessageEvent messageEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();

        if (!(messageEvent.getUser() instanceof GroupXiaomingUser))
            return;

        GroupXiaomingUser user = (GroupXiaomingUser) messageEvent.getUser();
        long group = user.getGroupCode();

        Message message = messageEvent.getMessage();
        String mes = message.serialize().toLowerCase(Locale.ROOT);

        final int botPermLevel = Objects.requireNonNull(xiaomingBot.getMiraiBot().getGroup(((GroupXiaomingUser) messageEvent.getUser()).getGroupCode())).getBotPermission().getLevel();
        final int memberPermLevel = user.getMemberContact().getPermission().getLevel();

        if (keys.containsKey(group)) {
            if (mes.contains("删除关键词") || mes.contains("添加关键词"))
                return;

            if (botPermLevel <= memberPermLevel)
                return;

            for (Pattern key : keys.get(group)) {
                if (key.matcher(mes).find())
                    try {
                        message.recall();
                        messageEvent.cancel();
                        user.mute(gmConfiguration.defaultMuteTime.get(group) * 60000);
                        xiaomingBot.getContactManager().sendGroupMessage(group,
                                new At(user.getCode()).serializeToMiraiCode() + " 你发送了一条违规消息");
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        xiaomingBot.getContactManager().sendGroupMessage(group, "撤回关键词失败：\n" + e);
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
                xiaomingBot.getContactManager().sendGroupMessage(group,
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
        if (gmConfiguration.getIgnoreUsers().contains(authorCode) || operatorCode == xiaomingBot.getCode())
            return;

        // 被撤回消息的id
        final int[] messageId = recall.getMessageIds();

        if (!gmConfiguration.getAntiRecall().containsKey(groupCode)
                || !gmConfiguration.getAntiRecall().get(groupCode)
                || gmConfiguration.getIgnoreUsers().contains(authorCode))
            return;

        // 获得被撤回的人最近发送的消息缓存
        final MessageEvent messageEvent = CollectionUtil.first(messageEvents, event -> {
            final XiaomingUser user = event.getUser();
            return user instanceof GroupXiaomingUser &&
                    ((GroupXiaomingUser) user).getGroupCode() == groupCode &&
                    Arrays.equals(messageId, event.getMessage().getMessageCode());
        });

        if (Objects.isNull(messageEvent)) {
            // message not found
            xiaomingBot.getContactManager().sendGroupMessage(groupCode, new At(operatorCode).serializeToMiraiCode()
                    + " 刚刚撤回了 " + new At(authorCode).serializeToMiraiCode() + " 的消息，但时间找不到");
        } else {
            final Message message = messageEvent.getMessage();

            xiaomingBot.getContactManager().sendGroupMessage(groupCode, new At(operatorCode).serializeToMiraiCode()
                    + " 刚刚撤回了 " + new At(authorCode).serializeToMiraiCode() + " 的消息：\n" + message.serialize());
        }
    }

    // 防闪照
    @EventListener
    public void antiFlash(@NotNull GroupMessageEvent groupMessageEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final Long group = groupMessageEvent.getGroup().getId();
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

        xiaomingBot.getContactManager().sendGroupMessage(group, new At(qq).serializeToMiraiCode() +
                " 发送了一张闪照，原图为：\n" + flashImage.getImage());

        xiaomingBot.getContactManager().getGroupContact(group).get().sendMessage(new PlainText(flashImage.toString()));
    }

    // 根据 miraiCode 发送闪照原图（仅限私聊）
    @EventListener
    public void flash(@NotNull MessageEvent messageEvent) {
        GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        XiaomingUser user = messageEvent.getUser();

        // 判断是否为私聊消息
        if (!(user instanceof PrivateXiaomingUser))
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
    }

    // 加群自动审核
    @EventListener
    public void autoVerify(MemberJoinRequestEvent requestEvent) {
        final GroupManagerConfiguration gmConfiguration = plugin.getGmConfig();
        final GroupManagerData gmData = plugin.getGmData();

        final long group = requestEvent.getGroupId();
        XiaomingUser owner = xiaomingBot.getContactManager().getGroupContact(group).get().getOwner().getUser();
        String request = requestEvent.getMessage();

        if (!gmConfiguration.getEnableAutoVerify().containsKey(group))
            return;

        if (gmData.autoVerify.get(group) == null
                || !gmConfiguration.getEnableAutoVerify().get(group)
                || !gmData.autoVerify.containsKey(group)) {
            owner.sendMessage("「" + requestEvent.getFromId()
                    + "(" + requestEvent.getFromNick() + ")」向你所管理的群聊「" + group + "("
                    + xiaomingBot.getContactManager().getGroupContact(group).get().getAlias() + "）」发送了一条加群请求，其具体内容为：\n"
                    + request);
            return;
        }

        for (String key : gmData.autoVerify.get(group)) {
            if (request.toLowerCase().contains(key.toLowerCase())) {
                try {
                    requestEvent.accept();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    xiaomingBot.getContactManager().sendGroupMessage(group, "加群自动审核时出现错误，请到后台查看报错");
                    return;
                }
            }
        }

        if (gmConfiguration.getAutoReject().get(group)) {
            requestEvent.reject(false, "回答错误，不予通过");
        } else {
            owner.sendMessage("「" + requestEvent.getFromId() + "("
                    + requestEvent.getFromNick() + ")」向你所管理的群聊「" + group + "("
                    + xiaomingBot.getContactManager().getGroupContact(group).get().getAlias() + "）」发送了一条加群请求，其具体内容为：\n"
                    + request);
        }
    }
}
