import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import { formatCounterQuantity, resolveCounterVisualMeta } from '../config/counterConfig.js';

/**
 * Mini-carta visual representando um tipo de contador acumulado sobre uma carta/herói.
 * Exibe exclusivamente a imagem do contador e o marcador numérico no formato "x(número)".
 */
export default class CounterCardPrefab extends Phaser.GameObjects.Container {
    constructor(scene, x, y, counterDto, scale = 1) {
        super(scene, x, y);
        this.scene = scene;

        const meta = resolveCounterVisualMeta(counterDto);
        this.counterType = meta.counterType;
        this.counterMeta = meta;
        this.quantity = Math.max(0, Math.floor(Number(counterDto?.quantity ?? 0)));

        const { width, height } = BOARD_LAYOUT.COUNTER_CARD; // 38 x 44
        this.cardW = width;
        this.cardH = height;

        const accentInt = CounterCardPrefab.parseHexColor(meta.accentColor, 0x38bdf8);

        this.shadow = scene.add.rectangle(2, 2, width, height, 0x05070b, 0.72);
        this.bg = scene.add.rectangle(0, 0, width, height, 0x0b101b, 1)
            .setStrokeStyle(1.8, accentInt);

        this.bottomStripe = scene.add.rectangle(0, (height / 2) - 2, width - 3, 3, accentInt, 1);

        // Imagem do tipo de contador posicionada na porção visível sob a base do Herói
        const textureKey = CounterCardPrefab.resolveTextureKey(scene, meta.iconKey);
        if (textureKey) {
            this.iconSprite = scene.add.image(-8.5, 9.5, textureKey).setDisplaySize(17, 17);
        } else {
            this.iconSprite = scene.add.circle(-8.5, 9.5, 6.5, accentInt, 1)
                .setStrokeStyle(1.2, 0x07090e);
        }

        // Quantidade no formato x(número)
        this.quantityText = scene.add.text(17, 10.5, formatCounterQuantity(this.quantity), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '10.5px',
            color: '#ffffff',
            fontStyle: 'bold',
            stroke: '#07090e',
            strokeThickness: 3,
            align: 'right'
        }).setOrigin(1, 0.5).setResolution(2);

        this.add([
            this.shadow,
            this.bg,
            this.bottomStripe,
            this.iconSprite,
            this.quantityText
        ]);

        this.setSize(width, height);
        this.setScale(scale);
        scene.add.existing(this);
    }

    static resolveTextureKey(scene, iconKey) {
        if (!scene || !scene.textures) return null;
        if (iconKey && scene.textures.exists(iconKey)) return iconKey;
        if (scene.textures.exists('counter_generic')) return 'counter_generic';
        return null;
    }

    static parseHexColor(hexString, fallback = 0x38bdf8) {
        if (typeof hexString === 'number' && Number.isFinite(hexString)) {
            return hexString;
        }
        const cleaned = String(hexString || '').replace('#', '').trim();
        const parsed = Number.parseInt(cleaned, 16);
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    updateQuantity(newQty) {
        const safeQty = Math.max(0, Math.floor(Number(newQty) || 0));
        this.quantity = safeQty;
        if (this.quantityText) {
            this.quantityText.setText(formatCounterQuantity(safeQty));
        }
    }

    playPulse() {
        if (!this.scene || !this.scene.tweens) return;
        const baseScaleX = this.scaleX;
        const baseScaleY = this.scaleY;
        this.scene.tweens.killTweensOf(this);
        this.scene.tweens.add({
            targets: this,
            scaleX: baseScaleX * 1.18,
            scaleY: baseScaleY * 1.18,
            duration: 130,
            yoyo: true,
            ease: 'Back.easeOut'
        });
    }
}
