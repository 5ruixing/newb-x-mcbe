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

  // ✅ 发光逻辑提前到光照之前，只对「基础颜色」做发光
  float diff = v_color0.a - 0.99;
  float mask = 1.0 - smoothstep(-0.0001, 0.0001, diff);
  vec3 baseColor = albedo.rgb;
  albedo.rgb = mix(baseColor, baseColor * 3.0, mask); // 发光倍率仍用2.0

  albedo.rgb *= albedo.rgb * v_light.rgb; // 环境光照只作用于「已经处理好发光的基础色」
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  gl_FragColor = albedo;
}