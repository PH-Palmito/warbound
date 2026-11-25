package com.warbound.model;

/**
 * Representa uma carta do catálogo. Modelo somente de exibição: nenhuma
 * regra (dano, cura, custo etc.) é calculada aqui — isso é responsabilidade
 * do backend. O frontend só guarda os dados para desenhar a carta.
 */
public class Carta {

    private final String id;
    private final String nome;
    private final Classe classe;
    private final TipoCarta tipo;
    private final int custo;
    private final Integer ataque;  // null para cartas sem ATQ/HP (Efeito, Busca)
    private final Integer hp;      // null para cartas sem ATQ/HP
    private final String descricao;
    private final int copias;      // quantidade de cópias no baralho comum
    private final String imagePath; // ilustração do personagem (produzida pelo Emanoel)

    public Carta(String id, String nome, Classe classe, TipoCarta tipo, int custo,
                 Integer ataque, Integer hp, String descricao, int copias, String imagePath) {
        this.id = id;
        this.nome = nome;
        this.classe = classe;
        this.tipo = tipo;
        this.custo = custo;
        this.ataque = ataque;
        this.hp = hp;
        this.descricao = descricao;
        this.copias = copias;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public Classe getClasse() { return classe; }
    public TipoCarta getTipo() { return tipo; }
    public int getCusto() { return custo; }
    public Integer getAtaque() { return ataque; }
    public Integer getHp() { return hp; }
    public String getDescricao() { return descricao; }
    public int getCopias() { return copias; }

    /** Caminho em resources, ex: "/assets/characters/templarios_cavaleiro.png". */
    public String getImagePath() { return imagePath; }

    public boolean isPersonagem() {
        return tipo == TipoCarta.PERSONAGEM;
    }
}
