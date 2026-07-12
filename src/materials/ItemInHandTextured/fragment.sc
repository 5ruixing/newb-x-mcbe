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

  albedo.rgb *= albedo.rgb * v_light.rgb;

  albedo.rgb *= nlEntityEdgeHighlight(v_edgemap);
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  // 对齐作者渐变发光逻辑：252(0.9882)=100%，253(0.9922)=50%，>253=0
  float alphaTex = albedo.a;
  // smoothstep(低阈值,高阈值,输入)，输入越小输出越大，实现从1降到0
  float glowFactor = 1.0 - smoothstep(0.9882, 0.9922, alphaTex);
  albedo.rgb = mix(albedo.rgb, albedo.rgb * 4.5, glowFactor);

  gl_FragColor = albedo;
}