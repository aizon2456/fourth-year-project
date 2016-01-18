package wong.ian.augmentedrealitysystem20;

/**
 * Created by Phelius on 12/30/2015.
 */
public class ChemicalContainer {
    private String location = "";
    private String room = "";
    private String cabinet = "";
    private String chemicalName = "";
    private int flammability = 0;
    private int health = 0;
    private int instability = 0;
    private String notice = "";

    public ChemicalContainer(String location, String room, String cabinet, String chemicalName) {
        this.location = location;
        this.room = room;
        this.cabinet = cabinet;
        this.chemicalName = chemicalName;
    }

    public int getFlammability() {
        return flammability;
    }

    public void setFlammability(int flammability) {
        this.flammability = flammability;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getInstability() {
        return instability;
    }

    public void setInstability(int instability) {
        this.instability = instability;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getLocation() {
        return location;
    }

    public String getRoom() {
        return room;
    }

    public String getCabinet() {
        return cabinet;
    }

    public String getChemicalName() {
        return chemicalName;
    }
}
