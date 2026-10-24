import Phaser from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export default class LoginScene extends Phaser.Scene {
    constructor() {
        super({ key: 'LoginScene' });
    }

    preload() {
        this.load.setPath('assets');
        if (!this.textures.exists('ui_p5_crt_button')) {
            this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        }
        if (!this.textures.exists('ui_card_frame_phantom')) {
            this.load.svg('ui_card_frame_phantom', 'images/ui/card_frame_phantom.svg', { width: 320, height: 440 });
        }
        if (!this.textures.exists('ui_arena_crt_radar')) {
            this.load.svg('ui_arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        }
    }

    create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        const { width, height } = this.scale;

        this.add.text(width / 2, height / 2 - 140, 'LENDAS DA POEIRA', {
            fontSize: '42px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.add.text(width / 2, height / 2 - 90, 'Digite seu nick para entrar no jogo:', {
            fontSize: '16px', color: '#94a3b8'
        }).setOrigin(0.5);

        // Input HTML integrado via DOM Element do Phaser
        const htmlInput = '<input type="text" id="nick-input" class="phantom-crt-input" placeholder="Seu Nick..." style="width: 320px; height: 50px; font-size: 20px; padding: 10px; text-align: center; outline: none;" />';
        this.inputElement = this.add.dom(width / 2, height / 2 - 20).createFromHTML(htmlInput);

        // Botão Entrar
        const btnBg = this.add.rectangle(width / 2, height / 2 + 60, 320, 55, 0x16a34a)
            .setStrokeStyle(2, 0xd4af37)
            .setInteractive({ useHandCursor: true });

        const btnTxt = this.add.text(width / 2, height / 2 + 60, 'ENTRAR NO JOGO', {
            fontSize: '18px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        btnBg.on('pointerdown', () => this.handleLogin());
    }

    async handleLogin() {
        const inputElem = document.getElementById('nick-input');
        const nick = inputElem ? inputElem.value.trim() : '';

        if (!nick) {
            alert('Por favor, digite um nick válido!');
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/api/players/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nick })
            });

            if (response.ok) {
                const player = await response.json();
                localStorage.setItem('lendas_player', JSON.stringify(player));
                this.scene.start('MainMenuScene', { player });
            } else {
                alert('Erro ao realizar login no servidor.');
            }
        } catch (e) {
            console.warn('Servidor offline. Usando perfil offline local...');
            const offlinePlayer = {
                uuid: 'local-uuid-test',
                name: nick,
                decks: []
            };
            localStorage.setItem('lendas_player', JSON.stringify(offlinePlayer));
            this.scene.start('MainMenuScene', { player: offlinePlayer });
        }
    }
}