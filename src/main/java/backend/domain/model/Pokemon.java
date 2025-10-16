package backend.domain.model;

public class Pokemon {
    private int id;
    private String name;
    private String form;
    private String type1;
    private String type2;
    private int total;
    private int hp;
    private int attack;
    private int defense;
    private int spAtk;
    private int spDef;
    private int speed;
    private int generation;

    public Pokemon(int id, String name, String form, String type1, String type2,
                   int total, int hp, int attack, int defense, int spAtk, int spDef, int speed, int generation) {
        this.id = id;
        this.name = name;
        this.form = form;
        this.type1 = type1;
        this.type2 = type2;
        this.total = total;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.spAtk = spAtk;
        this.spDef = spDef;
        this.speed = speed;
        this.generation = generation;
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getForm() {
        return form;
    }
    public String getType1() {
        return type1;
    }
    public String getType2() {
        return type2;
    }
    public int getTotal() {
        return total;
    }
    public int getHp() {
        return hp;
    }
    public int getAttack() {
        return attack;
    }
    public int getDefense() {
        return defense;
    }
    public int getSpAtk() {
        return spAtk;
    }
    public int getSpDef() {
        return spDef;
    }
    public int getSpeed() {
        return speed;
    }
    public int getGeneration() {
        return generation;
    }
}
