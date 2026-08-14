#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);

  float nlHash11(float x) {
    return fract(sin(x*127.1)*43758.5453123);
  }

  float nlEndStreakLayer(vec2 uv, float t, float density, float speed) {
    float col = floor(uv.x*density);
    float rnd = nlHash11(col);
    float cellX = fract(uv.x*density) - 0.5;
    float headY = fract(rnd*13.0 - t*speed*(0.5+rnd));
    float len = 0.08 + 0.12*nlHash11(col*3.7);
    float dist = headY - uv.y;
    float streak = 0.0;
    if (dist > 0.0 && dist < len) {
      streak = 1.0 - dist/len;
      streak *= smoothstep(0.03, 0.0, abs(cellX));
    }
    return streak;
  }
#endif

void main() {
  #ifndef INSTANCING
    vec3 dir = normalize(v_posTime.xyz);
    vec2 uv = vec2(atan2(dir.x, dir.z)*0.15915494 + 0.5, acos(dir.y)*0.31830989);

    vec4 diffuse = texture2D(s_SkyTexture, uv);

    vec3 color = diffuse.rgb + NL_ENDSKY_GLOW_INTENSITY*diffuse.rgb*diffuse.rgb;

    vec2 screenUV = gl_FragCoord.xy / u_viewRect.zw;
    float streakTime = v_posTime.w*NL_END_STREAKS_SPEED;
    float streaks = nlEndStreakLayer(screenUV, streakTime, NL_END_STREAKS_DENSITY, 1.0);
    streaks += 0.6*nlEndStreakLayer(screenUV*1.3+0.37, streakTime, NL_END_STREAKS_DENSITY*1.6, 1.4);
    vec3 streakCol = mix(vec3(0.55,0.35,1.0), vec3(1.0), min(streaks,1.0));
    color += streaks*streakCol*NL_END_STREAKS_INTENSITY;

    color = colorCorrection(color);

    gl_FragColor = vec4(color, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
