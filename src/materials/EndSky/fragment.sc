#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);
#endif

void main() {
  #ifndef INSTANCING
    vec3 dir = normalize(v_posTime.xyz);
    vec2 uv = vec2(atan2(dir.x, dir.z)*0.15915494 + 0.5, acos(dir.y)*0.31830989);

    vec4 diffuse = texture2D(s_SkyTexture, uv);

    vec3 color = diffuse.rgb + NL_ENDSKY_GLOW_INTENSITY*diffuse.rgb*diffuse.rgb;
    color = colorCorrection(color);

    gl_FragColor = vec4(color, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
