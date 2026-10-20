import Phaser from 'phaser';

export default class LoginScene extends Phaser.Scene {
    constructor() {
        super({ key: 'LoginScene' });
    }

    create() {
        const { width, height } = this.scale;

        this.add.text(width / 2, height / 2 - 140, 'LENDAS DA POEIRA', {
            fontSize: '42px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.add.text(width / 2, height / 2 - 90, 'Digite seu nick para entrar no jogo:', {
            fontSize: '16px', color: '#94a3b8'
        }).setOrigin(0.5);

        // Input HTML integrado via DOM Element do Phaser
        const htmlInput = '<input type="text" id="nick-input" placeholder="Seu Nick..." style="width: 320px; height: 50px; font-size: 20px; padding: 10px; background: #0f172a; color: #38bdf8; border: 2px solid #38bdf8; border-radius: 8px; text-align: center; outline: none;" />';
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