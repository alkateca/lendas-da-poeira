import Phaser from 'phaser';
import { BOARD_LAYOUT } from './config/BoardCoordinates.js';
import LoginScene from './scenes/LoginScene.js';
import MainMenuScene from './scenes/MainMenuScene.js';
import PvpLobbyScene from './scenes/PvpLobbyScene.js';
import DeckBuilderScene from './scenes/DeckBuilderScene.js';
import CollectionScene from './scenes/CollectionScene.js';
import ShopScene from './scenes/ShopScene.js';
import MatchScene from './scenes/MatchScene.js';

const globalStyle = document.createElement('style');
globalStyle.innerHTML = `
    html, body {
        margin: 0 !important;
        padding: 0 !important;
        width: 100vw !important;
        height: 100vh !important;
        overflow: hidden !important;
        background-color: #181c24;
    }
    #game-container {
        position: fixed !important;
        inset: 0 !important;
        width: 100vw !important;
        height: 100vh !important;
        margin: 0 !important;
        padding: 0 !important;
        overflow: hidden !important;
    }
    #game-container canvas {
        width: 100vw !important;
        height: 100vh !important;
        display: block !important;
    }
`;
document.head.appendChild(globalStyle);

const config = {
    type: Phaser.AUTO,
    parent: 'game-container',
    width: BOARD_LAYOUT.WIDTH,
    height: BOARD_LAYOUT.HEIGHT,
    backgroundColor: BOARD_LAYOUT.COLORS.BG_DARK,
    fps: {
        target: 60,
        smoothStep: true
    },
    render: {
        antialias: true,
        powerPreference: 'high-performance',
        roundPixels: false
    },
    scale: {
        mode: Phaser.Scale.NONE,
        width: BOARD_LAYOUT.WIDTH,
        height: BOARD_LAYOUT.HEIGHT
    },
    dom: {
        createContainer: true
    },
    scene: [LoginScene, MainMenuScene, PvpLobbyScene, DeckBuilderScene, CollectionScene, ShopScene, MatchScene]
};

const game = new Phaser.Game(config);

function syncFullViewportScale() {
    if (!game || !game.scale || !game.canvas) return;
    const vw = window.innerWidth;
    const vh = window.innerHeight;

    // Atualiza o tamanho de exibição e a proporção de input do Phaser para ocupar 100% da janela sem barras
    game.scale.displaySize.setSize(vw, vh);
    game.scale.displayScale.set(BOARD_LAYOUT.WIDTH / vw, BOARD_LAYOUT.HEIGHT / vh);
    game.scale.updateBounds();

    // Sincroniza o container de elementos DOM (inputs do Login e filtros do DeckBuilder) na mesma proporção
    const domContainer = game.domContainer;
    if (domContainer) {
        const scaleX = vw / BOARD_LAYOUT.WIDTH;
        const scaleY = vh / BOARD_LAYOUT.HEIGHT;
        domContainer.style.width = `${BOARD_LAYOUT.WIDTH}px`;
        domContainer.style.height = `${BOARD_LAYOUT.HEIGHT}px`;
        domContainer.style.transformOrigin = '0 0';
        domContainer.style.transform = `scale(${scaleX}, ${scaleY})`;
    }
}

game.events.once('ready', () => {
    syncFullViewportScale();
});
window.addEventListener('resize', syncFullViewportScale);
document.addEventListener('fullscreenchange', () => {
    setTimeout(syncFullViewportScale, 50);
});
