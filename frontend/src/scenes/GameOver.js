import { Scene } from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export class GameOver extends Scene
{
    constructor ()
    {
        super('GameOver');
    }

    create ()
    {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.cameras.main.setBackgroundColor(0xff0000);

        const { width, height } = this.scale;
        this.add.image(width / 2, height / 2, 'background').setAlpha(0.5);
        this.add.text(width / 2, height / 2, 'Game Over', {
            fontFamily: 'Arial Black', fontSize: 64, color: '#ffffff',
            stroke: '#000000', strokeThickness: 8,
            align: 'center'
        }).setOrigin(0.5);

        this.input.once('pointerdown', () => {

            this.scene.start('MainMenu');

        });
    }
}
