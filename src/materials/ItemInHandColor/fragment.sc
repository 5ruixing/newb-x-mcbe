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
    albedo = applyColorChange(albedo, ChangeColor.rgb, albedo.a);
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

  // 同步降低发光倍率
  float alphaTex = v_color0.a;
  float mask = 1.0 - smoothstep(0.9922, 0.9923, alphaTex);
  albedo.rgb = mix(albedo.rgb, albedo.rgb * 2.0, mask);

  gl_FragColor = albedo;
}