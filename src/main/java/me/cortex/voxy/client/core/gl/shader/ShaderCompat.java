package me.cortex.voxy.client.core.gl.shader;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Ajustes opcionais no GLSL para drivers/traducoes (ex.: MobileGlues no Android).
 * Tudo vem desligado por padrao; ative com argumentos da JVM (Zalith -> JVM arguments):
 *
 *   -Dvoxy.glsl.version="430 core"              troca a linha #version
 *   -Dvoxy.glsl.stripExt=GL_ARB_gpu_shader_int64,GL_KHR_shader_subgroup_basic
 *                                                comenta essas linhas #extension
 *   -Dvoxy.shader.dump=true                     grava dump de TODO shader (nao so os com aviso)
 */
final class ShaderCompat {
    private static final String VERSION_OVERRIDE = System.getProperty("voxy.glsl.version");
    private static final Set<String> STRIP_EXT = Arrays.stream(System.getProperty("voxy.glsl.stripExt", "").split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    static final boolean DUMP_ALL = Boolean.getBoolean("voxy.shader.dump");

    private static final Pattern VERSION = Pattern.compile("(?m)^[ \\t]*#version[ \\t]+\\d+([ \\t]+\\w+)?");
    private static final Pattern EXTENSION = Pattern.compile("(?m)^[ \\t]*#extension[ \\t]+(\\w+)[ \\t]*:[^\\n]*$");

    private ShaderCompat() {}

    static String apply(String src) {
        if (VERSION_OVERRIDE != null && !VERSION_OVERRIDE.isBlank()) {
            // Mantem o numero de linhas (o Builder injeta os #define depois da 1a linha)
            src = VERSION.matcher(src).replaceFirst(Matcher.quoteReplacement("#version " + VERSION_OVERRIDE.trim()));
        }
        if (!STRIP_EXT.isEmpty()) {
            src = EXTENSION.matcher(src).replaceAll(m ->
                    STRIP_EXT.contains(m.group(1))
                            ? Matcher.quoteReplacement("// [stripped] " + m.group().trim())
                            : Matcher.quoteReplacement(m.group()));
        }
        return src;
    }
}
