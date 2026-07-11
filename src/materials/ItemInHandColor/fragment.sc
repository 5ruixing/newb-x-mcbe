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
    gl_FragColor = vec4(0.0);
    return;
  #endif

  vec4 albedo = vec4(mix(vec3(1.0, 1.0, 1.0), v_color0.rgb, ColorBased.x), 1.0);

  #ifdef MULTI_COLOR_TINT
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
  #else
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a = ChangeColor.a;
  #endif

  albedo = applyOverlayColor(albedo, OverlayColor);

  #ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
      discard;
    }
  #endif

  // 修复拆分写法，规避X3014编译错误
  float step1 = step(0.9875, v_color0.a);
  float step2 = step(0.9925, v_color0.a);
  float isGlowPixel = step1 * (1.0 - step2);
  vec3 baseColor = albedo.rgb;

  albedo.rgb *= albedo.rgb * v_light.rgb;

  vec3 glowColor = baseColor * 8.0;
  albedo.rgb = mix(albedo.rgb, glowColor, isGlowPixel);

  albedo.rgb = mix(albedo.rgb, v_fog.rgb, v_fog.a);
  albedo.rgb = colorCorrection(albedo.rgb);

  gl_FragColor = albedo;
}