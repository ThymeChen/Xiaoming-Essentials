package cn.thymechen.xiaoming;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.debug.PluginDebugger;
import cn.chuanwise.xiaoming.debug.PluginDebuggerBuilder;
import cn.chuanwise.xiaoming.launcher.XiaoMingLauncher;
import cn.chuanwise.xiaoming.plugin.PluginHandler;
import org.fusesource.jansi.Ansi;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class EssentialsDebugger {
    public static void main(String[] args) {
        PluginDebugger debugger = new PluginDebuggerBuilder()
                .code(334378441)
                .password("060030zyc.")
                .workingDirectory(new File("E:/xiaoming-bot"))
                .addPlugin("Essentials", EssentialsPlugin.class)
                .build();

        try{
            XiaoMingLauncher launcher = debugger.getLauncher();
            // 无法启动 debugger 的原因是无法使用滑块验证

            if (!launcher.launch()) {
                throw new ExceptionInInitializerError();
            }

            launcher.getXiaoMingBot().getMiraiBot().getConfiguration().setWorkingDir(new File("E:/xiaoming-bot/launcher"));
            launcher.getXiaoMingBot().getMiraiBot().getConfiguration().fileBasedDeviceInfo();

            launcher.start();
            if (!launcher.launch()){
                throw new ExceptionInInitializerError();
            }

            XiaoMingBot xiaoMingBot = launcher.getXiaoMingBot();
            List<PluginHandler> pluginHandlers = debugger.getPluginHandlers();
            xiaoMingBot.getPluginManager().addPlugins(pluginHandlers);
            pluginHandlers.forEach(handler -> {
                if (xiaoMingBot.getPluginManager().loadPlugin(handler) && xiaoMingBot.getPluginManager().enablePlugin(handler)) {
                    System.out.println("enable plugin successfully!");
                }
            });
        }catch (Throwable e) {
            e.printStackTrace();
        }

//        Terminal terminal = TerminalBuilder.builder()
//                .system(true)
//                .build();
//
//        LineReader lineReader = LineReaderBuilder.builder()
//                .terminal(terminal)
//                .build();
//
//        final Thread thread = new Thread(() -> {
//            while (true) {
//                try {
//                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()));
//                    Thread.sleep(1000);
//                } catch (Exception e) {
//                    break;
//                }
//            }
//        });
//        thread.start();
//
//        String prompt = "> ";
//        while (true) {
//            String line;
//            lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
//            lineReader.getTerminal().writer().flush();
//
//            terminal.output().write();
//            terminal.flush();
//
//            line = lineReader.readLine(prompt);
//            System.out.println(prompt + line);
//
//            if (line.equals("stop")) {
//                thread.interrupt();
//                return;
//            }
//        }

//
//        int a = 5;
//        String verifyCode = "23456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
//
//        char[] verify = new char[a];
//        for (int i = 0; i < a; i++)
//            verify[i] = verifyCode.charAt(new Random().nextInt(verifyCode.length()));
//        String verifyStr = String.valueOf(verify);
//        System.out.println(verifyStr);
//
//        BufferedImage image = VerifyImage.getImage(verify);
//        System.out.println(image);
//
//        try(
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream)
//        ) {
//            ImageIO.write(image, "jpeg", imageOutputStream);
//
//            try(
//                    InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
//                    ExternalResource externalResource = ExternalResource.create(inputStream)
//            ) {
//                System.out.println(externalResource);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
    }
}
