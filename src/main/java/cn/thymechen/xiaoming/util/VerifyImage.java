package cn.thymechen.xiaoming.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class VerifyImage {
    private static int weight = 25;            // 验证码每个字符的长度和宽度
    private static final int height = 40;
    private static final Random r = new Random();    // 获取随机数对象
    private static final String[] fontNames = {"宋体", "华文楷体", "黑体", "微软雅黑", "楷体_GB2312", "Georgia"};   //字体数组

    /**
     * 获取随机的颜色
     *
     * @return Color
     */
    private static @NotNull Color randomColor() {
        int R = r.nextInt(225);     // 这里为什么是225，因为当r，g，b都为255时，即为白色，为了好辨认，需要颜色深一点。
        int G = r.nextInt(225);
        int B = r.nextInt(225);
        return new Color(R, G, B);              // 返回一个随机颜色
    }

    /**
     * 获取随机字体
     *
     * @return Font
     */
    private static @NotNull Font randomFont() {
        int index = r.nextInt(fontNames.length);    // 获取随机的字体
        String fontName = fontNames[index];
        int style = r.nextInt(4);            // 随机获取字体的样式，0是无样式，1是加粗，2是斜体，3是加粗加斜体
        int size = r.nextInt(10) + 24;       // 随机获取字体的大小
        return new Font(fontName, style, size);     // 返回一个随机的字体
    }

    /**
     * 画干扰线，验证码干扰线用来防止计算机解析图片
     *
     * @param image
     */
    private static void drawLine(@NotNull BufferedImage image) {
        int num = r.nextInt(6) + 4;    //定义干扰线的数量
        Graphics2D g = (Graphics2D) image.getGraphics();
        for (int i = 0; i < num; i++) {
            int x1 = r.nextInt(weight);
            int y1 = r.nextInt(height);
            int x2 = r.nextInt(weight);
            int y2 = r.nextInt(height);
            g.setColor(randomColor());
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 创建图片的方法
     *
     * @return BufferedImage
     */
    private static @NotNull BufferedImage createImage() {
        //创建图片缓冲区
        BufferedImage image = new BufferedImage(weight, height, BufferedImage.TYPE_INT_RGB);
        //获取画笔
        Graphics2D g = (Graphics2D) image.getGraphics();
        //设置背景色随机
        g.setColor(new Color(235, 235, r.nextInt(235) + 20));
        g.fillRect(0, 0, weight, height);
        //返回一个图片
        return image;
    }

    /**
     * 获取验证码图片的方法
     *
     * @return BufferedImage
     */
    public static @NotNull BufferedImage getImage(char @NotNull [] chars) {
        weight = weight * chars.length;

        BufferedImage image = createImage();
        Graphics2D g = (Graphics2D) image.getGraphics();    //获取画笔
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (char ch : chars) {
            String s = ch + "";       //随机生成字符，因为只有画字符串的方法，没有画字符的方法，所以需要将字符变成字符串再画
            sb.append(s);                           //添加到StringBuilder里面
            float x = i * 1.0F * weight / chars.length;            //定义字符的x坐标
            g.setFont(randomFont());                //设置字体，随机
            g.setColor(randomColor());              //设置颜色，随机
            g.drawString(s, x, height - 5);
            ++i;
        }

        // 用来保存验证码的文本内容
        String text = sb.toString();
        drawLine(image);
        return image;
    }
}
