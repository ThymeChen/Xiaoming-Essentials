package cn.chuanwise.xiaoming.essentials.listener;

import cn.chuanwise.toolkit.sized.SizedCopyOnWriteArrayList;
import cn.chuanwise.util.MapUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.essentials.EssentialsPlugin;
import cn.chuanwise.xiaoming.essentials.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.InteractEvent;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.listener.ListenerPriority;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreManagerListener extends SimpleListeners<EssentialsPlugin> {
    CoreManagerConfiguration coreConfiguration;

    Map<Long, List<InteractEvent>> callLimit = new HashMap<>();

    @Override
    public void onRegister() {
        coreConfiguration = plugin.getCoreConfig();
    }

    // 明确调用
    @EventListener(priority = ListenerPriority.HIGH)
    public void onMessageEvent(MessageEvent messageEvent) {
        final XiaomingUser user = messageEvent.getUser();
        if (!(user instanceof GroupXiaomingUser) || !coreConfiguration.isEnableClearCall())
            return;

        final String commandHead = coreConfiguration.getClearCall();
        if (StringUtil.isEmpty(commandHead))
            return;

        String groupTag = coreConfiguration.getGroupTag();
        if (StringUtil.isEmpty(groupTag)) {
            coreConfiguration.groupTag = "clear-call";
            getLogger().error("明确调用生效的群聊标签为空，将使用默认标签「" + coreConfiguration.getGroupTag() + "」");
            xiaomingBot.getFileSaver().readyToSave(coreConfiguration);
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
        XiaomingUser user = interactEvent.getContext().getUser();
        final int maxSize = coreConfiguration.getCallLimit().getMaxCall();

        if (!(user instanceof GroupXiaomingUser) || !coreConfiguration.getCallLimit().isEnableCallLimit())
            return;

        final long group = ((GroupXiaomingUser) user).getGroupCode();
        List<InteractEvent> limit = MapUtil.getOrPutSupply(callLimit, group, () -> new SizedCopyOnWriteArrayList<>(maxSize));

        if (!limit.isEmpty()) {
            final long sysTime = System.currentTimeMillis();
            final long maxTime = sysTime - limit.get(0).getContext().getMessage().getTime();
            final long minTime = sysTime - limit.get(limit.size() - 1).getContext().getMessage().getTime();

            if (minTime < TimeUnit.SECONDS.toMillis(coreConfiguration.getCallLimit().getCooldown())) {
                interactEvent.cancel();
                user.sendMessage("调用未冷却");
            } else if (limit.size() == coreConfiguration.getCallLimit().getMaxCall()
                    && maxTime > TimeUnit.SECONDS.toMillis(coreConfiguration.getCallLimit().getPeriod())) {
                limit.remove(0);
                limit.add(interactEvent);
            } else if (limit.size() == coreConfiguration.getCallLimit().getMaxCall()
                    && maxTime < TimeUnit.SECONDS.toMillis(coreConfiguration.getCallLimit().getPeriod())) {
                interactEvent.cancel();
                user.sendMessage("已达本群调用限制");
            } else
                limit.add(interactEvent);
        } else
            limit.add(interactEvent);
    }
}
