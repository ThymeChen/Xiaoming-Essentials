package cn.thymechen.xiaoming.listener;

import cn.chuanwise.toolkit.sized.SizedCopyOnWriteArrayList;
import cn.chuanwise.util.Maps;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.event.SendMessageEvent;
import cn.thymechen.xiaoming.EssentialsPlugin;
import cn.thymechen.xiaoming.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.InteractEvent;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.listener.ListenerPriority;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.Getter;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.GroupMessageSyncEvent;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.MessageChain;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
public class CoreManagerListener extends SimpleListeners<EssentialsPlugin> {
    public static Map<Long, List<InteractEvent>> callLimit = new HashMap<>();

    StopWatch watch = new StopWatch();

    private void watch() {
        xiaoMingBot.getScheduler().run(() -> {
            if (watch.getTime() == 0) {
                watch.start();

                while (watch.getTime() < TimeUnit.MINUTES.toMillis(1)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
                watch.reset();
            }
        });
    }

    // 使bot能够响应自身发出的指令
    @EventListener
    public void selfInteract(GroupMessageSyncEvent event) {
        CoreManagerConfiguration coreConfig = plugin.getCoreConfig();
        if (!coreConfig.isSelfInteract())
            return;
        try {
            MessageChain messageChain = event.getMessage();
            GroupContact target = xiaoMingBot.getContactManager().getGroupContact(event.getGroup().getId()).get();
            SendMessageEvent sendMessageEvent = new SendMessageEvent(target, messageChain);
//            xiaoMingBot.getInteractorManager().interact(xiaoMingBot.getConsoleXiaoMingUser(), sendMessageEvent.getMessageChain());
            sendMessageEvent.getMessageBox().nextValue(5 * 1000)
                    .ifPresent(message -> xiaoMingBot.getInteractorManager().interact(xiaoMingBot.getConsoleXiaoMingUser(), message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 明确调用
    @EventListener(priority = ListenerPriority.HIGH)
    public void onMessageEvent(@NotNull MessageEvent messageEvent) {
        CoreManagerConfiguration coreConfiguration = plugin.getCoreConfig();
        final XiaoMingUser user = messageEvent.getUser();
        if (!(user instanceof GroupXiaoMingUser) || !coreConfiguration.isEnableClearCall())
            return;

        final String commandHead = coreConfiguration.getClearCall();
        if (StringUtil.isEmpty(commandHead))
            return;

        String groupTag = coreConfiguration.getGroupTag();
        if (StringUtil.isEmpty(groupTag)) {
            coreConfiguration.groupTag = "clear-call";
            getLogger().error("明确调用生效的群聊标签为空，将使用默认标签「" + coreConfiguration.getGroupTag() + "」");
            xiaoMingBot.getFileSaver().readyToSave(coreConfiguration);
            return;
        }

        if (!user.getContact().hasTag(groupTag))
            return;

        final Message message = messageEvent.getMessage();
        final String serializedOriginalMessage = message.serializeOriginalMessage();
        if (!serializedOriginalMessage.startsWith(commandHead)) {
            messageEvent.cancel();
            return;
        }

        final String actualMessage = serializedOriginalMessage.substring(commandHead.length());
        final MessageChain messageChain = MiraiCode.deserializeMiraiCode(actualMessage);
        message.setMessageChain(messageChain);
    }

    // 调用限制
    @EventListener(priority = ListenerPriority.HIGH)
    public void callLimit(InteractEvent interactEvent) {
        CoreManagerConfiguration coreConfiguration = plugin.getCoreConfig();
        XiaoMingUser user = interactEvent.getContext().getUser();
        final int maxSize = coreConfiguration.getCallLimit().getMaxCall();

        if (!(user instanceof GroupXiaoMingUser) || !coreConfiguration.getCallLimit().isEnableCallLimit())
            return;

        final long group = ((GroupXiaoMingUser) user).getGroupCode();
        List<InteractEvent> limit = Maps.getOrPutSupply(callLimit, group, () -> new SizedCopyOnWriteArrayList<>(maxSize));

        if (!limit.isEmpty()) {
            final long sysTime = System.currentTimeMillis();
            final long maxTime = sysTime - limit.get(0).getContext().getMessage().getTime();
            final long minTime = sysTime - limit.get(limit.size() - 1).getContext().getMessage().getTime();

            if (minTime <= TimeUnit.SECONDS.toMillis(coreConfiguration.getCallLimit().getCooldown())) {
                interactEvent.cancel();
                if (watch.getTime() == 0)
                    user.sendMessage("调用未冷却");
                watch();
            } else if (limit.size() >= coreConfiguration.getCallLimit().getMaxCall()
                    && maxTime <= TimeUnit.SECONDS.toMillis(coreConfiguration.getCallLimit().getPeriod())) {
                interactEvent.cancel();
                if (watch.getTime() == 0)
                    user.sendMessage("已达本群调用限制");
                watch();
            } else {
                limit.add(interactEvent);
            }
        } else
            limit.add(interactEvent);
    }
}
