package com.listen.portfolio.common.util;

import java.util.Locale;

/**
 * Dynamic Multi-Language Localization and Graceful Fallback Utility.
 *
 * <p>Design Rationale:
 * <ul>
 *   <li><b>Context-Driven Resolution</b>: Inspects the resolved {@link Locale} (originating from HTTP
 *       {@code Accept-Language} header and maintained in Spring's {@code LocaleContextHolder}).</li>
 *   <li><b>Priority & Fallback Hierarchy</b>:
 *       <ol>
 *         <li>If {@code locale} matches Chinese ({@code "zh"}), returns {@code zhVal} if present.</li>
 *         <li>If {@code locale} matches Japanese ({@code "ja"}), returns {@code jaVal} if present.</li>
 *         <li>Otherwise, or if the localized version is blank/null, gracefully falls back to {@code defaultVal} (English).</li>
 *       </ol>
 *   </li>
 *   <li><b>Zero JOIN Overhead</b>: Operates on flat entity columns (e.g. {@code title_zh}, {@code title_ja})
 *       instead of complex relational joins, maximizing database read performance.</li>
 * </ul>
 */
public class I18nUtils {

    private I18nUtils() {
    }

    /**
     * Resolves the localized representation of a given field, returning the target language version
     * if available, or gracefully falling back to the default (English) representation.
     *
     * @param defaultVal Default baseline text (English).
     * @param zhVal Chinese translation text (nullable or blank).
     * @param jaVal Japanese translation text (nullable or blank).
     * @param locale Target locale resolved from the client's request context.
     * @return The best-matching localized string.
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
