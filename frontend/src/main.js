import Phaser from 'phaser';
import { BOARD_LAYOUT } from './config/BoardCoordinates.js';
import LoginScene from './scenes/LoginScene.js';
import MainMenuScene from './scenes/MainMenuScene.js';
import PvpLobbyScene from './scenes/PvpLobbyScene.js';
import DeckBuilderScene from './scenes/DeckBuilderScene.js';
import ShopScene from './scenes/ShopScene.js';
import MatchScene from './scenes/MatchScene.js';

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
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH
    },
    dom: {
        createContainer: true
    },
    scene: [LoginScene, MainMenuScene, PvpLobbyScene, DeckBuilderScene, ShopScene, MatchScene]
};

new Phaser.Game(config);
