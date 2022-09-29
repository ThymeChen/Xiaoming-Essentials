package cn.thymechen.xiaoming;

import cn.chuanwise.xiaoming.plugin.JavaPlugin;
import cn.thymechen.xiaoming.configuration.coreManagerConfiguration.CoreManagerConfiguration;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerConfiguration;
import cn.thymechen.xiaoming.configuration.groupManagerConfiguration.GroupManagerData;
import cn.thymechen.xiaoming.interactor.CoreManagerInteractor;
import cn.thymechen.xiaoming.interactor.GroupManagerInteractor;
import cn.thymechen.xiaoming.interactor.RemoteInteractor;
import cn.thymechen.xiaoming.listener.CoreManagerListener;
import cn.thymechen.xiaoming.listener.GroupManagerListeners;
import lombok.Getter;
import net.mamoe.mirai.contact.Group;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@SuppressWarnings("all")
public class EssentialsPlugin extends JavaPlugin {
    public static final EssentialsPlugin INSTANCE = new EssentialsPlugin();

    GroupManagerConfiguration gmConfig;
    GroupManagerData gmData;
    CoreManagerConfiguration coreConfig;

    public List<Long> getIgnoreUsers() {
        return INSTANCE.getGmConfig().getIgnoreUsers();
    }

    public void onLoad() {
        loadConfig();

        coreConfig = setupConfiguration(CoreManagerConfiguration.class, new File(getDataFolder(), "core.json"), CoreManagerConfiguration::new);
        gmConfig = setupConfiguration(GroupManagerConfiguration.class, new File(getDataFolder(), "config.json"), GroupManagerConfiguration::new);
        gmData = setupConfiguration(GroupManagerData.class, new File(getDataFolder(), "data.json"), GroupManagerData::new);

        xiaoMingBot.getEventManager().registerListeners(new CoreManagerListener(), this);
        xiaoMingBot.getEventManager().registerListeners(new GroupManagerListeners(), this);

        if (Objects.isNull(coreConfig.getClearCall()))
            coreConfig.setClearCall("");

//        new Thread(() -> {
//            xiaoMingBot.stop();
//            xiaoMingBot.start();
//        }).start();
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
        banPlugins();
        mute();

        xiaoMingBot.getInteractorManager().registerInteractors(new CoreManagerInteractor(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new GroupManagerInteractor(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new RemoteInteractor(), this);

        xiaoMingBot.getFileSaver().readyToSave(gmConfig);
        xiaoMingBot.getFileSaver().readyToSave(gmData);
        xiaoMingBot.getFileSaver().readyToSave(coreConfig);
    }

    private void loadConfig() {
        try {
            File oldFolder = null;
            boolean hasOldData = false;

            for (File file : Objects.requireNonNull(xiaoMingBot.getPluginManager().getDirectory().listFiles()))
                if (file.isDirectory() && file.toString().contains("essentials")) {
                    hasOldData = true;
                    oldFolder = file;
                    break;
                }

            if (hasOldData) {
                getLogger().warn("发现旧数据文件夹 essentials, 开始迁移 Essentials 的数据...");
                if (oldFolder.renameTo(getDataFolder())) {
                    getLogger().warn("数据迁移成功!");
                } else {
                    getLogger().error("数据迁移失败, 请手动将文件夹 essentials 更名为 Essentials 后重启小明!");
                }
            } else {
                getDataFolder().mkdir();
            }
        } catch (Exception ignored) {

        }
    }

    @Deprecated
    private void moveConfig() {
        try {
            File oldFolder = new File(xiaoMingBot.getPluginManager().getDirectory(), "essentials");

            if (System.getProperty("os.name").startsWith("Windows")) {
                boolean ess = false;
                for (File file : Objects.requireNonNull(xiaoMingBot.getPluginManager().getDirectory().listFiles())) {
                    if (file.toString().endsWith("essentials")) {
                        ess = true;
                        break;
                    }
                }
                if (ess) {
                    getLogger().warn("当前系统为 Windows 系统, 将执行特殊检测方案...");
                    getLogger().info(" ");
                    getLogger().error("旧数据存在, 请手动将文件夹 essentials 更名为 Essentials 后重启小明!");
                    getLogger().info(" ");
                    getLogger().error("小明将于 10 秒后关闭...");

                    xiaoMingBot.getScheduler().runLater(10 * 1000, () -> xiaoMingBot.stop());
                } else {
                    getDataFolder().mkdir();
                    coreConfig = setupConfiguration(CoreManagerConfiguration.class, new File(getDataFolder(), "core.json"), CoreManagerConfiguration::new);
                    gmConfig = setupConfiguration(GroupManagerConfiguration.class, new File(getDataFolder(), "config.json"), GroupManagerConfiguration::new);
                    gmData = setupConfiguration(GroupManagerData.class, new File(getDataFolder(), "data.json"), GroupManagerData::new);
                    return;
                }
            } else {
                if (oldFolder.exists() && oldFolder.isDirectory()) {
                    getLogger().warn("发现旧数据文件夹 essentials, 开始迁移 Essentials 的数据...");
                    File oldCore = new File(oldFolder, "core.json");
                    File oldConfig = new File(oldFolder, "config.json");
                    File oldData = new File(oldFolder, "data.json");

                    // 迁移 core
                    if (oldCore.exists()) {
                        File newCore = new File(getDataFolder(), "core.json");
                        Files.copy(oldCore.toPath(), newCore.toPath());
                        oldCore.delete();
                    } else
                        getLogger().error("配置文件 core.json 不存在, 将忽略该配置的迁移!");
                    // 迁移 config
                    if (oldConfig.exists()) {
                        File newConfig = new File(getDataFolder(), "config.json");
                        Files.copy(oldConfig.toPath(), newConfig.toPath());
                        oldConfig.delete();
                    } else
                        getLogger().error("配置文件 config.json 不存在, 将忽略该配置的迁移!");
                    //迁移 data
                    if (oldData.exists()) {
                        File newData = new File(getDataFolder(), "data.json");
                        Files.copy(oldData.toPath(), newData.toPath());
                        oldData.delete();
                    } else
                        getLogger().error("配置文件 data.json 不存在, 将忽略该配置的迁移!");

                    getLogger().warn("数据迁移成功!");
                } else {
                    getDataFolder().mkdir();
                    coreConfig = setupConfiguration(CoreManagerConfiguration.class, new File(getDataFolder(), "core.json"), CoreManagerConfiguration::new);
                    gmConfig = setupConfiguration(GroupManagerConfiguration.class, new File(getDataFolder(), "config.json"), GroupManagerConfiguration::new);
                    gmData = setupConfiguration(GroupManagerData.class, new File(getDataFolder(), "data.json"), GroupManagerData::new);
                    return;
                }
            }
        } catch (Exception e) {
            getLogger().error("数据迁移失败! ", e);
            getLogger().warn("开始加载旧数据...");
        }

        coreConfig = setupConfiguration(CoreManagerConfiguration.class, new File(getDataFolder(), "core.json"), CoreManagerConfiguration::new);
        gmConfig = setupConfiguration(GroupManagerConfiguration.class, new File(getDataFolder(), "config.json"), GroupManagerConfiguration::new);
        gmData = setupConfiguration(GroupManagerData.class, new File(getDataFolder(), "data.json"), GroupManagerData::new);
    }

    private void initConfig() {
        for (Group group : xiaoMingBot.getMiraiBot().getGroups()) {
            long groupId = group.getId();
            if (!gmConfig.getDefaultMuteTime().containsKey(groupId))
                gmConfig.getDefaultMuteTime().put(groupId, 10);

            if (!gmConfig.getAutoReject().containsKey(groupId))
                gmConfig.getAutoReject().put(groupId, false);

            if (!gmConfig.getVerifyConfig().getJoinVerify().containsKey(groupId))
                gmConfig.getVerifyConfig().getJoinVerify().put(groupId, false);
        }
    }

    private void banPlugins() {
        xiaoMingBot.getScheduler().runAtFixedRateLater(TimeUnit.SECONDS.toMillis(30), 5 * 1000, () -> {
            for (String name : coreConfig.getBannedPlugins()) {
                if (xiaoMingBot.getPluginManager().isEnabled(name))
                    xiaoMingBot.getPluginManager().disablePlugin(name);
            }
        });
    }

    private void mute() {
        xiaoMingBot.getScheduler().runAtFixedRateLater(TimeUnit.MINUTES.toMillis(1), 10 * 1000, () -> {
            for (long groupCode : gmConfig.getMuteForever().keySet()) {
                xiaoMingBot.getContactManager().getGroupContact(groupCode)
                        .ifPresent(group -> {
                            for (long qq : gmConfig.getMuteForever().get(groupCode)) {
                                group.getMember(qq)
                                        .ifPresent(member -> {
                                            if (!member.isMuted())
                                                member.mute(TimeUnit.DAYS.toMillis(30));
                                        });
                            }
                        });
            }
        });
    }
}
