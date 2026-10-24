import Phaser from 'phaser';

const fragShader = `
#define SHADER_NAME PHANTOM_CRT_FS
precision mediump float;

uniform sampler2D uMainSampler;
uniform vec2 uResolution;
uniform float uTime;
uniform float uChromaticAberration;

varying vec2 outTexCoord;

vec2 curveUV(vec2 uv) {
    uv = uv * 2.0 - 1.0;
    vec2 offset = abs(uv.yx) / vec2(7.5, 7.5);
    uv = uv + uv * offset * offset;
    uv = uv * 0.5 + 0.5;
    return uv;
}

void main() {
    vec2 uv = curveUV(outTexCoord);

    // Bordas fora do tubo CRT ficam pretas
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        gl_FragColor = vec4(0.02, 0.02, 0.03, 1.0);
        return;
    }

    // Aberração Cromática (separa R, G, B nas bordas ou durante impactos)
    float aber = 0.0012 + uChromaticAberration;
    float r = texture2D(uMainSampler, vec2(uv.x + aber, uv.y)).r;
    float g = texture2D(uMainSampler, uv).g;
    float b = texture2D(uMainSampler, vec2(uv.x - aber, uv.y)).b;
    vec3 color = vec3(r, g, b);

    // Scanlines horizontais sutis
    float scanline = sin(uv.y * uResolution.y * 1.15 - uTime * 4.0) * 0.045;
    color -= scanline;

    // Vinheta de monitor antigo
    float vig = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vig = clamp(pow(16.0 * vig, 0.22), 0.0, 1.0);
    color *= vig;

    gl_FragColor = vec4(color, 1.0);
}
`;

export default class CRTPostFXPipeline extends Phaser.Renderer.WebGL.Pipelines.PostFXPipeline {
    constructor(game) {
        super({
            game,
            renderTarget: true,
            fragShader
        });
        this.aberrationAmount = 0;
    }

    onPreRender() {
        this.set2f('uResolution', this.renderer.width, this.renderer.height);
        this.set1f('uTime', this.game.loop.time / 1000);
        this.set1f('uChromaticAberration', this.aberrationAmount);
    }

    pulseGlitch(intensity = 0.012, duration = 220) {
        this.aberrationAmount = intensity;
        this.game.scene.scenes.forEach(s => {
            if (s.sys.isActive()) {
                s.tweens.addCounter({
                    from: intensity,
                    to: 0,
                    duration,
                    onUpdate: (tween) => {
                        this.aberrationAmount = tween.getValue();
                    }
                });
            }
        });
    }
}
