package cn.thymechen.xiaoming.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplate {
    Map<String, Object> paramMap;

    String prefix;
    String suffix;
    String originalPrefix;

    String patternContent = "[\\w:]+";
    String patternPrefix;
    String patternSuffix;

    Pattern placeholderPattern;
    Pattern regexPattern = Pattern.compile("[\\*\\.\\?\\+\\^\\$\\|\\\\\\/\\[\\]\\(\\)\\{\\}]");

    public StringTemplate(Map<String, Object> paramMap) {
        this(paramMap, "${", "}");
    }

    public StringTemplate(Map<String, Object> paramMap, String prefix, String suffix) {
        this(paramMap, prefix, suffix, "original");
    }

    public StringTemplate(Map<String, Object> paramMap, String prefix, String suffix, String originalPrefix) {
        this.paramMap = paramMap;
        this.prefix = prefix;
        this.suffix = suffix;
        this.originalPrefix = originalPrefix;

        StringBuilder sb = new StringBuilder();
        for (char ch : prefix.toCharArray()) {
            String str = String.valueOf(ch);

            if (regexPattern.matcher(str).matches()) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        this.patternPrefix = sb.toString();

        sb = new StringBuilder();
        for (char ch : suffix.toCharArray()) {
            String str = String.valueOf(ch);

            if (regexPattern.matcher(str).matches()) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        this.patternSuffix = sb.toString();

        placeholderPattern = Pattern.compile(this.patternPrefix + this.patternContent + this.patternSuffix);
    }

    /**
     * 设置模板前缀
     *
     * @param prefix 前缀
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        StringBuilder sb = new StringBuilder();
        for (char ch : prefix.toCharArray()) {
            String str = String.valueOf(ch);

            if (regexPattern.matcher(str).matches()) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        this.patternPrefix = sb.toString();
        placeholderPattern = Pattern.compile(this.patternPrefix + patternContent + this.patternSuffix);
    }

    /**
     * 设置模板后缀
     *
     * @param suffix 后缀
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
        StringBuilder sb = new StringBuilder();
        for (char ch : suffix.toCharArray()) {
            String str = String.valueOf(ch);

            if (regexPattern.matcher(str).matches()) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        this.patternSuffix = sb.toString();
        placeholderPattern = Pattern.compile(this.patternPrefix + patternContent + this.patternSuffix);
    }

    /**
     * 设置变量中可以接受的字符串
     *
     * @param patternContent 可接受字符串的正则表达式
     */
    public void setPatternContent(String patternContent) {
        this.patternContent = patternContent;
    }

    /**
     * 设置模板需要输出原始内容的前缀
     *
     * @param originalPrefix 前缀
     */
    public void setOriginalPrefix(String originalPrefix) {
        this.originalPrefix = originalPrefix;
    }

    /**
     * 渲染模板
     *
     * @param template 字符串模版
     * @return 解析后的字符串
     */
    public String replace(String template) {
        if (template == null || paramMap == null)
            return null;
        StringBuilder sb = new StringBuilder();
        Matcher matcher = this.placeholderPattern.matcher(template);

        while (matcher.find()) {
            String param = matcher.group();     // 提取变量
            String key = matcher.group().substring(this.prefix.length(), param.length() - this.suffix.length());    // 提取关键字
            Object value = paramMap.get(key);   // 获取关键字对应的值（可为 null）

            // 变量替换
            if (key.startsWith(originalPrefix + ":")) {
                matcher.appendReplacement(sb, patternPrefix + key.substring(originalPrefix.length() + 1) + suffix);
            } else if (key.startsWith(originalPrefix.charAt(0) + ":")) {
                matcher.appendReplacement(sb, patternPrefix + key.substring(2) + suffix);
            } else {
                matcher.appendReplacement(sb, value == null ? "\"null\"" : value.toString());
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
