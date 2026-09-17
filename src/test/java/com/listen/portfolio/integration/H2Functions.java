package com.listen.portfolio.integration;

/**
 * H2 内存数据库专有 SQL 兼容函数适配器 (H2 Custom Function Adapter)。
 *
 * <p><b>一、核心设计理念与背景：</b>
 * <ul>
 *   <li><b>MySQL 大小写敏感比对兼容：</b>
 *       在生产环境的 MySQL 中，{@code userMapper.findByNameCaseSensitive(username)} 使用了
 *       {@code WHERE BINARY name = #{username}} 来区分大小写（例如区分 "Listen" 与 "listen"）。</li>
 *   <li><b>H2 语法桥接方案：</b>
 *       H2 内存数据库不包含名为 {@code BINARY} 的内置函数。为了在不需要修改任何业务 SQL 的前提下，
 *       使单元测试能在 H2 纯内存库中绿色跑通，
 *       在 {@link BaseIntegrationTest#initH2Functions()} 中通过 H2 扩展语法：
 *       <pre>{@code
 *       CREATE ALIAS IF NOT EXISTS "BINARY" DETERMINISTIC FOR "com.listen.portfolio.integration.H2Functions.binary"
 *       }</pre>
 *       将 SQL 中的 {@code BINARY(x)} 调用直接路由到本类的静态方法 {@link #binary(String)}。</li>
 *   <li><b>运行时行为：</b>
 *       本方法作为直通恒等函数（Identity Function），直接原样返回入参字符串，
 *       配合 H2 默认的严格大小写比对，完美模拟 MySQL 的 {@code BINARY} 算子行为。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see BaseIntegrationTest
 */
public class H2Functions {

    /**
     * H2 自定义函数映射实现：模拟 MySQL BINARY 算子。
     *
     * @param value 输入字符串
     * @return 原样返回输入字符串
     */
    public static String binary(String value) {
        return value;
    }
}
