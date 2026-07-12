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
    albedo = applyColorChange(albedo, ChangeColor.rgb, albedo.a);
  #endif

  albedo.rgb *= mix(vec3_splat(1.0), v_color0.rgb, ColorBased.x);
  albedo = applyOverlayColor(albedo, OverlayColor);

  albedo.rgb *= albedo.rgb * v_light.rgb;

  albedo.rgb *= nlEntityEdgeHighlight(v_edgemap);
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  // 发光倍率从 4.5 降到 2.0（可再调为 1.5/1.8 等）
  float alphaTex = albedo.a;
  float mask = 1.0 - smoothstep(0.9922, 0.9923, alphaTex);
  albedo.rgb = mix(albedo.rgb, albedo.rgb * 2.0, mask);

  gl_FragColor = albedo;
}