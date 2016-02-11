package wong.ian.augmentedrealitysystem20;

/**
 * Contains all the information regarding a chemical container.
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

    /**
     * Creates a chemical container.
     * @param flammability
     * @param health
     * @param instability
     * @param notice Information pertinent to the chemical and the user.
     * @param chemicalName
     */
    public ChemicalContainer(int flammability, int health, int instability, String notice, String chemicalName) {
        this.flammability = flammability;
        this.health = health;
        this.instability = instability;
        this.notice = notice;
        this.chemicalName = chemicalName;
    }

    public int getFlammability() {
        return flammability;
    }

    public int getHealth() {
        return health;
    }

    public int getInstability() {
        return instability;
    }

    public String getNotice() {
        return notice;
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
