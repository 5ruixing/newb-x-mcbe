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
  albedo.rgb *= albedo.rgb * v_light.rgb;
  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  // 【替换原来发白的4行代码，最小改动，无if无布尔，兼容s_5_0】
  float diff = v_color0.a - 0.99;
  float mask = 1.0 - smoothstep(-0.0001, 0.0001, diff);
  albedo.rgb = mix(albedo.rgb * 4.5, albedo.rgb, mask);

  gl_FragColor = albedo;
}