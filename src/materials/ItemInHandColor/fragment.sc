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
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    return;
  #endif
  vec4 albedo = vec4(mix(vec3(1.0, 1.0, 1.0), v_color0.rgb, ColorBased.x), 1.0);
  #ifdef MULTI_COLOR_TINT
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
  #else
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a *= ChangeColor.a;
  #endif
  albedo = applyOverlayColor(albedo, OverlayColor);
  #ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
      discard;
    }
  #endif

  // 严格限定：仅 v_color0.a = 252(≈0.9882) 发光
  float diff = v_color0.a - 0.9882;
  float mask = smoothstep(-0.0001, 0.0001, diff);
  vec3 baseRaw = albedo.rgb;

  // 环境光衰减
  float envAvg = dot(v_light.rgb, vec3(1.0)) / 3.0;
  float glowFade = 1.0 - smoothstep(0.3, 0.6, envAvg);

  // 发光强度
  vec3 emissivePath = baseRaw * 1.2 * glowFade;

  // 普通像素光照流程
  vec3 litPath = baseRaw;
  litPath *= litPath * v_light.rgb;
  litPath = mix(litPath, v_fog.rgb, v_fog.a);
  litPath = colorCorrection(litPath);

  albedo.rgb = mix(litPath, emissivePath, mask);
  gl_FragColor = albedo;
}