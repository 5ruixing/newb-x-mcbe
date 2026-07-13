$input v_color0, v_fog, v_light, v_texcoord0, v_edgemap
#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <newb/main.sh>
uniform vec4 ChangeColor;
uniform vec4 OverlayColor;
uniform vec4 ColorBased;
uniform vec4 MatColor;
uniform vec4 MultiplicativeTintColor;
SAMPLER2D_AUTOREG(s_MatTexture);
void main() {
  #if defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4_splat(0.0);
    return;
  #endif
  vec4 albedo = MatColor * texture2D(s_MatTexture, v_texcoord0);
  #ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
      discard;
    }
  #endif
  #ifdef MULTI_COLOR_TINT
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
  #else
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
  #endif
  albedo.rgb *= mix(vec3_splat(1.0), v_color0.rgb, ColorBased.x);
  albedo = applyOverlayColor(albedo, OverlayColor);

  // 严格限定：仅 252(0.9882) ~ 253(0.9922) 之间的像素发光
  float alphaTex = albedo.a;
  float mask = smoothstep(0.9881, 0.9882, alphaTex) * (1.0 - smoothstep(0.9922, 0.9923, alphaTex));
  vec3 baseRaw = albedo.rgb;

  // 环境光衰减（保持白天不发光、暗处发光的方块逻辑）
  float envAvg = dot(v_light.rgb, vec3(1.0)) / 3.0;
  float glowFade = 1.0 - smoothstep(0.3, 0.6, envAvg);

  // 发光强度（可改 1.2/1.5/1.8）
  vec3 emissivePath = baseRaw * 1.0 * glowFade;

  // 普通像素完整光照流程
  vec3 litPath = baseRaw;
  litPath *= litPath * v_light.rgb;
  litPath *= nlEntityEdgeHighlight(v_edgemap);
  litPath = mix(litPath, v_fog.rgb, v_fog.a);
  litPath = colorCorrection(litPath);

  // 二选一输出
  albedo.rgb = mix(litPath, emissivePath, mask);
  gl_FragColor = albedo;
}