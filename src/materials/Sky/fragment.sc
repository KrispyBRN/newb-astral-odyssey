#ifndef INSTANCING
  $input v_worldPos, v_underwaterRainTimeDay
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 TimeOfDay;
  uniform vec4 Day;
  uniform vec4 FogColor;
  uniform vec4 FogAndDistanceControl;

  #ifdef NL_AURORA_CURTAIN
    vec3 nlGetAuroraCurtain(vec3 vDir, float time, float dither) {
      float elev = clamp(vDir.y, 0.0, 1.0);
      if (elev < 0.04) return vec3(0.0);

      float ang = atan2(vDir.z, vDir.x);
      float t = time*NL_AURORA_CURTAIN_VELOCITY;

      // several drifting ribbon bands wrapped around the sky, each warped by noise so they wave like curtains
      float bands = 0.0;
      for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float freq = (2.0 + fi*1.4)/max(NL_AURORA_CURTAIN_SCALE, 0.001);
        float phase = t*(0.7 + 0.25*fi) + fi*2.4;
        float warp = 2.0*noise2D(vec2(ang*1.3 + fi*4.1 + dither, elev*2.5 - t*0.4)) - 1.0;
        float wave = sin(ang*freq + phase + warp*2.4);
        bands += pow(max(wave, 0.0), 1.0/max(NL_AURORA_CURTAIN_WIDTH, 0.01))/3.0;
      }

      // vertical falloff - fades in above the horizon, fades out before the zenith
      float vertical = smoothstep(0.04, 0.3, elev)*(1.0-smoothstep(0.55, 1.0, elev));

      // green low on the curtain blending to the upper colour higher up, like real aurora curtains
      vec3 col = mix(NL_AURORA_CURTAIN_COL1, NL_AURORA_CURTAIN_COL2, clamp(elev*2.2, 0.0, 1.0));

      return col*bands*vertical;
    }
  #endif
#endif

void main() {
  #ifndef INSTANCING
    vec3 viewDir = normalize(v_worldPos);

    nl_environment env;
    env.end = false;
    env.nether = false;
    env.underwater = v_underwaterRainTimeDay.x > 0.5;
    env.rainFactor = v_underwaterRainTimeDay.y;
    env.dayFactor = v_underwaterRainTimeDay.w;
    env.fogCol = FogColor.rgb;
    env = calculateSunParams(env, TimeOfDay.x, Day.x);

    nl_skycolor skycol = nlOverworldSkyColors(env);

    vec3 skyColor = nlRenderSky(skycol, env, -viewDir, v_underwaterRainTimeDay.z, true);
    #ifdef NL_SHOOTING_STAR
      skyColor += NL_SHOOTING_STAR*nlRenderShootingStar(viewDir, env.fogCol, v_underwaterRainTimeDay.z);
    #endif
    #ifdef NL_GALAXY_STARS
      skyColor += NL_GALAXY_STARS*nlRenderGalaxy(viewDir, env.fogCol, env, v_underwaterRainTimeDay.z);
    #endif
    #ifdef NL_AURORA_CURTAIN
      float auroraMask = (1.0-1.0*env.rainFactor)*max(1.0 - 3.0*max(env.fogCol.b, env.fogCol.g), 0.0);
      if (auroraMask > 0.01) {
        float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233)))*43758.5453);
        skyColor += NL_AURORA_CURTAIN*nlGetAuroraCurtain(viewDir, v_underwaterRainTimeDay.z, dither)*auroraMask;
      }
    #endif

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
