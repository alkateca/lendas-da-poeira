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

}
