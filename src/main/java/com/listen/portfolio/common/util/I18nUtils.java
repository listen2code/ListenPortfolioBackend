package com.listen.portfolio.common.util;

import java.util.Locale;

/**
 * 数据库多语言字段动态解析工具类
 */
public class I18nUtils {

    private I18nUtils() {
    }

    /**
     * 根据当前 Context 中的 Locale 选取对应语言版本的字段，优先返回目标语言，若为空则回退到默认（英文）版本。
     *
     * @param defaultVal 默认版本（英文）
     * @param zhVal      中文版本
     * @param jaVal      日语版本
     * @param locale     客户端请求传入的 Locale
     * @return 解析后的对应语言字符串
     */
    public static String getLocalizedText(String defaultVal, String zhVal, String jaVal, Locale locale) {
        if (locale == null) {
            return defaultVal;
        }
        String lang = locale.getLanguage().toLowerCase();
        if ("zh".equals(lang)) {
            return (zhVal != null && !zhVal.isBlank()) ? zhVal : defaultVal;
        } else if ("ja".equals(lang)) {
            return (jaVal != null && !jaVal.isBlank()) ? jaVal : defaultVal;
        }
        return defaultVal;
    }
}
