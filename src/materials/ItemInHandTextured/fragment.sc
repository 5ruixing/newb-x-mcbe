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

  // 保存原始底色用于独立发光层
  vec3 rawBase = albedo.rgb;

  // 标准完整光照流程（和方块基础光照逻辑对齐）
  albedo.rgb *= albedo.rgb * v_light.rgb;
  albedo.rgb *= nlEntityEdgeHighlight(v_edgemap);
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  // 复刻方块nlGlow衰减逻辑
  float envAvg = dot(v_light.rgb, vec3(1.0)) / 3.0;
  float glowFade = 1.0 - smoothstep(0.3, 0.6, envAvg);

  // 判定：贴图alpha ≤253发光
  float alphaTex = albedo.a;
  float mask = 1.0 - smoothstep(0.9922, 0.9923, alphaTex);
  vec3 emissive = rawBase * 1.8 * glowFade * mask;

  // 加法叠加发光，和方块 diffuse.rgb += glow 完全一致
  albedo.rgb += emissive;

  gl_FragColor = albedo;
}