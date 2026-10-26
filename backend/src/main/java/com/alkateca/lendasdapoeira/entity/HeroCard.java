package com.alkateca.lendasdapoeira.entity;

import com.alkateca.lendasdapoeira.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HeroCard extends Card{

    private int espirito;
    private int ataque;
    private int defesa;
    private int vidaMaxima;
    private int vidaAtual;
    private int reducaoDano;
    private int danoBonus;
    private int vulnerabilidade;
    private int maxWeaponSlots;

    private Boolean estaAtivo;
    private Boolean estaVivo;

    public void setAtaque(int ataque) {
        this.ataque = Math.max(0, ataque);
    }

    public void setDefesa(int defesa) {
        this.defesa = Math.max(0, defesa);
    }

    public void setEspirito(int espirito) {
        this.espirito = Math.max(0, espirito);
    }

    public void setVidaMaxima(int vidaMaxima) {
        this.vidaMaxima = Math.max(0, vidaMaxima);
    }

    public void setReducaoDano(int reducaoDano) {
        this.reducaoDano = Math.max(0, reducaoDano);
    }

    public void setVulnerabilidade(int vulnerabilidade) {
        this.vulnerabilidade = Math.max(0, vulnerabilidade);
    }

    public void setDanoBonus(int danoBonus) {
        this.danoBonus = Math.max(0, danoBonus);
    }

}
