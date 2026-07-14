$input v_color0, v_fog, v_light, v_glintuv
#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <newb/main.sh>
uniform vec4 ChangeColor;
uniform vec4 OverlayColor;
uniform vec4 GlintColor;
uniform vec4 MatColor;
uniform vec4 MultiplicativeTintColor;
uniform vec4 TileLightColor;
uniform vec4 ColorBased;
SAMPLER2D_AUTOREG(s_GlintTexture);
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

  // 【完全复制你稳定版的发光判定与分流逻辑】
  float diff = v_color0.a - 0.99;
  float mask = 1.0 - smoothstep(-0.0001, 0.0001, diff);
  vec3 baseRaw = albedo.rgb;
  // 发光管线：不受环境光影响，固定亮度
  vec3 emissivePath = baseRaw * 1.2;
  // 普通管线：完整原版光照+附魔流程
  vec3 litPath = baseRaw;

  // 原版附魔+光照计算（只在普通管线执行，发光像素跳过环境/附魔光照）
  vec4 light = nlGlint(v_light, v_glintuv, s_GlintTexture, GlintColor, TileLightColor, vec4(litPath,1.0));
  litPath *= litPath * light.rgb;
  litPath = mix(litPath, v_fog.rgb, v_fog.a);
  litPath = colorCorrection(litPath);

  // 二选一：发光像素直接用固定自发光，普通像素带附魔条纹
  albedo.rgb = mix(litPath, emissivePath, mask);

  gl_FragColor = albedo;
}