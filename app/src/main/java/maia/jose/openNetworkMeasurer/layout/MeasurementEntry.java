package maia.jose.openNetworkMeasurer.layout;

public class MeasurementEntry{
    private String title;
    private String subtitle;

    public MeasurementEntry(String a, String b){
        title = a;
        subtitle = b;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
