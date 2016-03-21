package wong.ian.augmentedrealitysystem20;

/**
 * Contains all the information regarding a chemical container.
 */
public class ChemicalContainer {
    private String location = null;
    private String room = null;
    private String cabinet = null;
    private String chemicalName = null;
    private int flammability = 0;
    private int health = 0;
    private int instability = 0;
    private String notice = null;

    /**
     * Creates a chemical container.
     */
    public ChemicalContainer() {
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

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getCabinet() {
        return cabinet;
    }

    public void setCabinet(String cabinet) {
        this.cabinet = cabinet;
    }

    public String getChemicalName() {
        return chemicalName;
    }

    public void setChemicalName(String chemicalName) {
        this.chemicalName= chemicalName;
    }
}
