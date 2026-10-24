import Phaser from 'phaser';

const fragShader = `
#define SHADER_NAME PHANTOM_CRT_FS
precision mediump float;

uniform sampler2D uMainSampler;
uniform vec2 uResolution;
uniform float uTime;
uniform float uChromaticAberration;

varying vec2 outTexCoord;

void main() {
    vec2 uv = outTexCoord;

    // Sem aberração cromática em repouso (0.0) para manter textos 100% nítidos!
    // Só aplica se houver um pulso de dano (uChromaticAberration > 0.0)
    vec3 color;
    if (uChromaticAberration > 0.0001) {
        float r = texture2D(uMainSampler, vec2(uv.x + uChromaticAberration, uv.y)).r;
        float g = texture2D(uMainSampler, uv).g;
        float b = texture2D(uMainSampler, vec2(uv.x - uChromaticAberration, uv.y)).b;
        color = vec3(r, g, b);
    } else {
        color = texture2D(uMainSampler, uv).rgb;
    }

    // Scanline ultra sutil (0.01) apenas no fundo, sem cortar letras pretas
    float scanline = sin(uv.y * uResolution.y * 1.5) * 0.01;
    color -= scanline;

    // Vinheta muito leve apenas nos extremos da tela
    float vig = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vig = clamp(pow(22.0 * vig, 0.12), 0.0, 1.0);
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

    pulseGlitch(intensity = 0.004, duration = 180) {
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
