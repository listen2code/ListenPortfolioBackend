package com.listen.portfolio.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("I18nUtils 单元测试")
class I18nUtilsTest {

    @Test
    @DisplayName("多语言回退与匹配逻辑全面测试")
    void testGetLocalizedText() {
        // Null locale falls back to default
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", null));

        // Chinese locale matches zh
        assertEquals("中文", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.CHINESE));
        assertEquals("中文", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.SIMPLIFIED_CHINESE));

        // Chinese with blank zh falls back to default
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "", "日本語", Locale.CHINESE));
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", null, "日本語", Locale.CHINESE));

        // Japanese locale matches ja
        assertEquals("日本語", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.JAPANESE));
        assertEquals("日本語", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.JAPAN));

        // Japanese with blank ja falls back to default
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", "", Locale.JAPANESE));
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", "   ", Locale.JAPANESE));
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", null, Locale.JAPANESE));

        // Other locales (English, French, etc.) return default
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.ENGLISH));
        assertEquals("Default EN", I18nUtils.getLocalizedText("Default EN", "中文", "日本語", Locale.FRENCH));
    }
}
