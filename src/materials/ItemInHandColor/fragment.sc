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
  // 仅252(0.9882)触发发光，原版透明玻璃冰块不会命中
  float diff = v_color0.a - 0.9882;
  float mask = 1.0 - smoothstep(-0.00005, 0.00005, diff);
  vec3 baseRaw = albedo.rgb;
  // 管线A：发光像素，完全不参与环境光照，固定亮度
  vec3 emissivePath = baseRaw * 1.2;
  // 管线B：普通像素，完整标准光照流程
  vec3 litPath = baseRaw;
  litPath *= litPath * v_light.rgb;
  litPath = mix(litPath, v_fog.rgb, v_fog.a);
  litPath = colorCorrection(litPath);
  // 二选一输出，无if，编译安全
  albedo.rgb = mix(litPath, emissivePath, mask);
  gl_FragColor = albedo;
}