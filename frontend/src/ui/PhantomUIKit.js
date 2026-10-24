import Phaser from 'phaser';

export default class PhantomUIKit {
    /**
     * Cria um botão poligonal assimétrico estilo Persona 5 com telemetria CRT.
     */
    static createRebelButton(scene, x, y, width, height, label, accentColor = 0xe60012, onClick) {
        const container = scene.add.container(x, y);
        const hw = width / 2;
        const hh = height / 2;

        // 1. Sombra dura deslocada (muda de posição no hover)
        const shadowPoly = scene.add.polygon(8, 8, [
            -hw + 12, -hh + 4,
             hw,      -hh,
             hw - 14,  hh,
            -hw,       hh - 4
        ], accentColor, 1).setOrigin(0);

        // 2. Placa principal chanfrada preta com contorno branco cortante
        const bgPoly = scene.add.polygon(0, 0, [
            -hw + 10, -hh,
             hw,      -hh + 4,
             hw - 12,  hh,
            -hw,       hh - 6
        ], 0x07090e, 0.96).setOrigin(0).setStrokeStyle(2.5, 0xf8fafc);

        // 3. Tag lateral estilo terminal ">_"
        const tagPoly = scene.add.polygon(0, 0, [
            -hw + 14, -hh + 4,
            -hw + 52, -hh + 4,
            -hw + 42,  hh - 8,
            -hw + 6,   hh - 8
        ], accentColor, 1).setOrigin(0);

        const promptTxt = scene.add.text(-hw + 28, 0, '>_', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        // 4. Texto principal com leve inclinação rebelde
        const txt = scene.add.text(14, 0, label.toUpperCase(), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#f8fafc',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        container.add([shadowPoly, bgPoly, tagPoly, promptTxt, txt]);

        // Zona de clique
        const hitZone = scene.add.rectangle(0, 0, width, height, 0xffffff, 0)
            .setInteractive({ useHandCursor: true });
        container.add(hitZone);

        hitZone.on('pointerover', () => {
            scene.tweens.killTweensOf(container);
            scene.tweens.add({
                targets: container,
                scaleX: 1.05,
                scaleY: 1.05,
                angle: -1.8,
                duration: 110,
                ease: 'Back.easeOut'
            });
            shadowPoly.setFillStyle(0xffe600, 1);
            shadowPoly.setPosition(13, 11);
            bgPoly.setStrokeStyle(3, 0x00f0ff);
            txt.setColor('#ffe600');
        });

        hitZone.on('pointerout', () => {
            scene.tweens.killTweensOf(container);
            scene.tweens.add({
                targets: container,
                scaleX: 1.0,
                scaleY: 1.0,
                angle: 0,
                duration: 140,
                ease: 'Sine.easeOut'
            });
            shadowPoly.setFillStyle(accentColor, 1);
            shadowPoly.setPosition(8, 8);
            bgPoly.setStrokeStyle(2.5, 0xf8fafc);
            txt.setColor('#f8fafc');
        });

        hitZone.on('pointerdown', () => {
            scene.tweens.add({
                targets: container,
                scaleX: 0.95,
                scaleY: 0.95,
                duration: 60,
                yoyo: true,
                onComplete: () => {
                    if (onClick) onClick();
                }
            });
        });

        return { container, bg: bgPoly, txt, shadow: shadowPoly };
    }

    /**
     * Dispara uma faixa diagonal cinética ("Cut-In" estilo Persona 5)
     * Ideal para mudanças de fase em MatchScene.js (CHOICE, PREPARATION, RESOLUTION, COMBAT)
     */
    static playPhaseCutIn(scene, phaseTitle, subText = 'SYS.EXEC // PROTOCOLO DE ARENA', colorHex = 0xe60012) {
        const cutInContainer = scene.add.container(960, 540).setDepth(280).setAngle(-7);

        const backSlash = scene.add.rectangle(0, 0, 2400, 150, 0x050507, 0.95)
            .setStrokeStyle(5, 0xf8fafc);
        const accentStripe = scene.add.rectangle(0, -52, 2400, 24, colorHex, 1);
        const bottomStripe = scene.add.rectangle(0, 58, 2400, 12, 0xffe600, 1);

        const mainTxt = scene.add.text(-900, -4, `// ${phaseTitle.toUpperCase()} //`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '46px',
            color: '#ffffff',
            fontStyle: 'italic bold',
            stroke: '#e60012',
            strokeThickness: 6
        }).setOrigin(0.5);

        const telemetryTxt = scene.add.text(900, 38, subText, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#00f0ff',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        cutInContainer.add([backSlash, accentStripe, bottomStripe, mainTxt, telemetryTxt]);
        cutInContainer.setScale(1, 0);

        // Abre a fenda diagonal
        scene.tweens.add({
            targets: cutInContainer,
            scaleY: 1,
            duration: 140,
            ease: 'Cubic.easeOut'
        });

        // Desliza o texto em alta velocidade para o centro e depois rasga para fora
        scene.tweens.add({
            targets: mainTxt,
            x: 0,
            duration: 200,
            ease: 'Expo.easeOut',
            onComplete: () => {
                scene.tweens.add({
                    targets: mainTxt,
                    x: 80,
                    duration: 450
                });
            }
        });

        scene.tweens.add({
            targets: telemetryTxt,
            x: 120,
            duration: 240,
            ease: 'Expo.easeOut'
        });

        // Fecha a fenda e destrói
        scene.tweens.add({
            targets: cutInContainer,
            scaleY: 0,
            alpha: 0,
            delay: 720,
            duration: 160,
            ease: 'Cubic.easeIn',
            onComplete: () => cutInContainer.destroy()
        });
    }
}
