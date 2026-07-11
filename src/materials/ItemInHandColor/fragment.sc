$input v_color0, v_fog, v_light
#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <newb/main.sh>

uniform vec4 ChangeColor;
uniform vec4 OverlayColor;
uniform vec4 ColorBased;
uniform vec4 MatColor;
uniform vec4 MultiplicativeTintColor;

void main() {
#if defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(0.0);
    return;
#endif

    // 修正：vec3 替代 float3，规范vec4构造
    vec3 baseMix = mix(vec3(1.0,1.0,1.0), v_color0.rgb, ColorBased.x);
    vec4 albedo = vec4(baseMix, 1.0);

#ifdef MULTI_COLOR_TINT
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#else
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a = ChangeColor.a;
#endif

    albedo = applyOverlayColor(albedo, OverlayColor);

#ifdef ALPHA_TEST
    if (albedo.a < 0.5) discard;
#endif

    albedo.rgb *= albedo.rgb * v_light.rgb;

    float glowMask = 1.0 - step(0.2, albedo.a);
    albedo.rgb += albedo.rgb * 6.0 * glowMask;

    albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
    albedo.rgb = colorCorrection(albedo.rgb);
    gl_FragColor = albedo;
}