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

  #ifdef NL_AURORA_3D
    float nlPow2(float x) { return x*x; }
    float nlClamp01(float x) { return clamp(x, 0.0, 1.0); }
    float nlSqrt1(float x) { return sqrt(max(x, 0.0)); }

    vec3 nlGetAurora3D(vec3 vDir, float time, float dither) {
      float VdotU = clamp(vDir.y, 0.0, 1.0);
      float visibility = nlSqrt1(nlClamp01(VdotU*4.5 - 0.225));
      visibility *= 4.0 - VdotU*0.9;
      if (visibility <= 1.0) return vec3(0.0);

      vec3 aurora = vec3(0.0);
      vec3 wpos = vDir;
      wpos.xz /= max(wpos.y, 0.1);
      vec2 cameraPosM = vec2(0.0);
      cameraPosM.x += time*10.0;

      const int sampleCount = 7;
      const int sampleCountP = sampleCount + 10;

      float ditherM = dither + 10.0;
      float auroraAnimate = time*0.0;

      for (int i = 0; i < sampleCount; i++) {
        float current = nlPow2((float(i) + ditherM) / float(sampleCountP));
        vec2 planePos = wpos.xz*(0.8 + current)*10.0 + cameraPosM;
        planePos *= 0.7;
        float noise = noise2D(planePos);
        noise = nlPow2(nlPow2(nlPow2(nlPow2(1.0 - 0.8*abs(noise - 0.5)))));
        noise *= noise2D(planePos*8.0 + auroraAnimate + vec2(37.0, 91.0));
        noise *= noise2D(planePos*1.0 - auroraAnimate + vec2(150.0, 61.0));
        float currentM = 1.0 - current;
        aurora += noise*currentM*mix(vec3(0.65, 0.48, 1.05), vec3(0.0, 4.5, 3.0), nlPow2(nlPow2(currentM)));
      }

      aurora *= 3.8;
      return aurora*visibility / float(sampleCount);
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
    #ifdef NL_AURORA_3D
      float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233)))*43758.5453);
      float aurora3DMask = (1.0-1.0*env.rainFactor)*max(1.0 - 3.0*max(env.fogCol.b, env.fogCol.g), 0.0);
      vec3 aurora3D = nlGetAurora3D(viewDir, v_underwaterRainTimeDay.z, dither)*aurora3DMask;
      skyColor += aurora3D;
    #endif

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
