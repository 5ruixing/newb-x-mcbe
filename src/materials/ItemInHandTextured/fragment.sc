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

  // ✅ 发光逻辑提前到光照之前
  float alphaTex = albedo.a;
  float mask = 1.0 - smoothstep(0.9922, 0.9923, alphaTex);
  vec3 baseColor = albedo.rgb;
  albedo.rgb = mix(baseColor, baseColor * 3.0, mask); // 发光倍率2.0

  albedo.rgb *= albedo.rgb * v_light.rgb; // 环境光照只作用于「已经处理好发光的基础色」
  albedo.rgb *= nlEntityEdgeHighlight(v_edgemap);
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  gl_FragColor = albedo;
}