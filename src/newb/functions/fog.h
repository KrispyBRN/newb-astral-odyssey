#ifndef FOG_H
#define FOG_H

float nlRenderFogFade(float relativeDist, vec3 FOG_COLOR, vec2 FOG_CONTROL) {
  #ifdef NL_FOG
    float fade = smoothstep(FOG_CONTROL.x, FOG_CONTROL.y, relativeDist);

    // misty effect
    float density = NL_MIST_DENSITY*(19.0 - 18.0*FOG_COLOR.g);
    fade += (1.0-fade)*(0.3-0.3*exp(-relativeDist*relativeDist*density));

    return NL_FOG * fade;
  #else
    return 0.0;
  #endif
}

vec2 nlRenderGodRayIntensity(vec3 cPos, vec3 worldPos, float t, vec2 uv1, float relativeDist, vec3 FOG_COLOR, float moonPhase, vec3 moonDir) {
  // offset wPos (only works upto 16 blocks)
  vec3 offset = cPos - 16.0*fract(worldPos*0.0625);
  offset = abs(2.0*fract(offset*0.0625)-1.0);
  offset = offset*offset*(3.0-2.0*offset);
  //offset = 0.5 + 0.5*cos(offset*0.392699082);

  //vec3 ofPos = wPos+offset;
  vec3 nrmof = normalize(worldPos);
  float diff = dot(offset,vec3(0.1,0.2,1.0)) + 0.07*t;

  float volS = 0.0;

  #ifdef NL_GODRAY
    // fixed world-axis shaft (the sun's compass bearing never rotates in vanilla, only its altitude does)
    float uS = nrmof.z/length(nrmof.zy);
    float maskS = nrmof.x*nrmof.x;
    volS = sin(7.0*uS + 1.5*diff)*sin(3.0*uS + diff);
    volS *= volS*maskS*uv1.y*(1.0-maskS*maskS);
    volS *= relativeDist*relativeDist;

    // dawn/dusk mask
    volS *= NL_GODRAY*clamp(3.0*(FOG_COLOR.r-FOG_COLOR.b), 0.0, 1.0);
    volS = smoothstep(0.0, 0.1, volS);
  #endif

  float volM = 0.0;

  #ifdef NL_GODRAY_FULLMOON
    // rotate the same shaft shape to follow the moon's actual horizontal bearing this frame
    vec3 moonAxis = normalize(vec3(moonDir.x, 0.0, moonDir.z));
    vec3 moonPerp = vec3(-moonAxis.z, 0.0, moonAxis.x);
    float rx = dot(nrmof, moonAxis);
    float rz = dot(nrmof, moonPerp);

    float uM = rz/length(vec2(rz, nrmof.y));
    float maskM = rx*rx;
    volM = sin(7.0*uM + 1.5*diff)*sin(3.0*uM + diff);
    volM *= volM*maskM*uv1.y*(1.0-maskM*maskM);
    volM *= relativeDist*relativeDist;

    // night mask (same fog-color brightness heuristic the 3D aurora uses)
    float nightMask = max(1.0 - 3.0*max(FOG_COLOR.b, FOG_COLOR.g), 0.0);
    // moon phase 0 = full moon (vanilla moon phase texture ordering, 8 phases, wraps around)
    float moonDist = min(moonPhase, 8.0 - moonPhase);
    float fullMoonMask = (1.0 - smoothstep(0.0, NL_GODRAY_FULLMOON_RANGE, moonDist))*nightMask;

    volM *= NL_GODRAY_FULLMOON*fullMoonMask;
    volM = smoothstep(0.0, 0.1, volM);
  #endif

  // x = combined ray intensity (drives fog alpha), y = full-moon-only intensity (drives moonlight color tint)
  return vec2(volS + volM, volM);
}

#endif
