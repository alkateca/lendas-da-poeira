import { Scene } from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export class Preloader extends Scene
{
    constructor ()
    {
        super('Preloader');
    }

    init ()
    {
        this.add.image(512, 384, 'background');
        this.add.rectangle(512, 384, 468, 32).setStrokeStyle(1, 0xffffff);
        const bar = this.add.rectangle(512 - 230, 384, 4, 28, 0xffffff);

        this.load.on('progress', (progress) => {
            bar.width = 4 + (460 * progress);
        });
    }

    preload ()
    {
        // Aponta para public/assets/
        this.load.setPath('assets');

        this.load.image('logo', 'logo.png');

        // Molduras legadas / fallback em /assets/images/
        this.load.image('card_back', 'images/modeloBaralho.png');
        this.load.image('card_discard_back', 'images/modeloBaralhoDescarte.png');

        // Novos SVGs temáticos (Persona 5 + CRT) em /assets/images/ui/
        this.load.svg('ui_card_frame_phantom', 'images/ui/card_frame_phantom.svg', { width: 320, height: 440 });
        this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        this.load.svg('ui_arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        this.load.svg('ui_graveyard_glitch', 'images/ui/graveyard_glitch.svg', { width: 82, height: 96 });

        // Sobrescreve também as chaves de molduras de carta caso queira que os prefabs usem o SVG direto:
        this.load.svg('card_frame_hero', 'images/ui/card_frame_hero.svg', { width: 280, height: 380 });
        this.load.svg('card_frame_playable', 'images/ui/card_frame_playable.svg', { width: 280, height: 380 });
    }

    create ()
    {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.scene.start('MainMenu');
    }
}