package wong.ian.augmentedrealitysystem20;

public class SyncInt {
    int value;

    public SyncInt(int value) {
        setValue(value);
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
