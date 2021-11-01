package cn.chuanwise.xiaoming.essentials;

import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.chuanwise.xiaoming.essentials.configuration.groupManagerConfiguration.GroupManagerData;
import cn.chuanwise.xiaoming.essentials.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.chuanwise.xiaoming.essentials.interactor.GroupManagerInteractor;
import cn.chuanwise.xiaoming.essentials.interactor.CoreManagerInteractor;
import cn.chuanwise.xiaoming.essentials.listener.GroupManagerListeners;
import cn.chuanwise.xiaoming.essentials.listener.CoreManagerListener;
import cn.chuanwise.xiaoming.group.GroupInformation;
import cn.chuanwise.xiaoming.plugin.JavaPlugin;
import lombok.Getter;
import net.mamoe.mirai.contact.Group;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Getter
public class EssentialsPlugin extends JavaPlugin {
    public static final EssentialsPlugin INSTANCE = new EssentialsPlugin();

    GroupManagerConfiguration gmConfig;
    GroupManagerData gmData;
    CoreManagerConfiguration coreConfig;

    public void onLoad() {
        getDataFolder().mkdirs();

        coreConfig = loadFileOrSupply(CoreManagerConfiguration.class, new File(getDataFolder(), "core.json"), CoreManagerConfiguration::new);
        gmConfig = loadFileOrSupply(GroupManagerConfiguration.class, new File(getDataFolder(), "config.json"), GroupManagerConfiguration::new);
        gmData = loadFileOrSupply(GroupManagerData.class, new File(getDataFolder(), "data.json"), GroupManagerData::new);

        xiaomingBot.getEventManager().registerListeners(new CoreManagerListener(), this);
        xiaomingBot.getEventManager().registerListeners(new GroupManagerListeners(), this);
    }

    @Override
    public void onEnable() {
        getLogger().info("\n\n" +
                " ███████╗ ███████╗ ███████╗\n" +
                " ██╔════╝ ██╔════╝ ██╔════╝\n" +
                " █████╗   ███████╗ ███████╗\n" +
                " ██╔══╝   ╚════██║ ╚════██║\n" +
                " ███████╗ ███████║ ███████║\n" +
                " ╚══════╝ ╚══════╝ ╚══════╝\n" +
                "                @Thyme_Chen\n");

        initConfig();
        xiaomingBot.getScheduler().runLater(5000, this::ignorePlugins);

        xiaomingBot.getInteractorManager().registerInteractors(new CoreManagerInteractor(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new GroupManagerInteractor(), this);

        xiaomingBot.getFileSaver().readyToSave(gmConfig);
        xiaomingBot.getFileSaver().readyToSave(gmData);
        xiaomingBot.getFileSaver().readyToSave(coreConfig);
    }

    @Override
    public void onDisable() {
        getXiaomingBot().getScheduler().stop();
    }

    private void initConfig() {
        for (Group group : xiaomingBot.getMiraiBot().getGroups()) {
            long groupId = group.getId();
            if (!gmConfig.getDefaultMuteTime().containsKey(groupId))
                gmConfig.getDefaultMuteTime().put(groupId, 10L);

            if (!gmConfig.getAutoReject().containsKey(groupId))
                gmConfig.getAutoReject().put(groupId, false);
        }
    }

    private void ignorePlugins() {
        while (true) {
            if (getXiaomingBot().isDisabled())
                return;
            for (String name : coreConfig.getBannedPlugins()) {
                if (xiaomingBot.getPluginManager().isLoaded(name))
                    xiaomingBot.getPluginManager().disablePlugin(name);
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(60));
            } catch (InterruptedException ignored) {

            }
        }
    }
}
