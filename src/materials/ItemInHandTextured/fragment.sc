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
  // 仅252~253区间像素发光，玻璃冰块alpha<252不会触发
  float alphaTex = albedo.a;
  float mask = smoothstep(0.9882, 0.9883, alphaTex) * (1.0 - smoothstep(0.9922, 0.9923, alphaTex));
  vec3 baseRaw = albedo.rgb;
  // 管线A：发光像素，完全无视环境光照，固定亮度
  vec3 emissivePath = baseRaw * 1.0;
  // 管线B：普通像素完整光照、描边、雾、调色
  vec3 litPath = baseRaw;
  litPath *= litPath * v_light.rgb;
  litPath *= nlEntityEdgeHighlight(v_edgemap);
  litPath = mix(litPath, v_fog.rgb, v_fog.a);
  litPath = colorCorrection(litPath);
  // 二选一输出
  albedo.rgb = mix(litPath, emissivePath, mask);
  gl_FragColor = albedo;
}