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

  vec3 rawBase = albedo.rgb;

  // 标准光照流程
  albedo.rgb *= albedo.rgb * v_light.rgb;
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  // 环境光衰减，复刻方块效果
  float envAvg = dot(v_light.rgb, vec3(1.0)) / 3.0;
  float glowFade = 1.0 - smoothstep(0.3, 0.6, envAvg);

  // 判定：v_color0.a ≤252发光
  float diff = v_color0.a - 0.99;
  float mask = 1.0 - smoothstep(-0.0001, 0.0001, diff);
  vec3 emissive = rawBase * 1.8 * glowFade * mask;

  // 加法叠加发光
  albedo.rgb += emissive;

  gl_FragColor = albedo;
}