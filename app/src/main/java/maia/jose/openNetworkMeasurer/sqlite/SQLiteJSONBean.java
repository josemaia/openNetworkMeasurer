package maia.jose.openNetworkMeasurer.sqlite;

import com.orm.SugarRecord;
public class SQLiteJSONBean extends SugarRecord<SQLiteJSONBean> {
    private String json;
    private Boolean hasSynchronized;

    public SQLiteJSONBean(){
    }


    public SQLiteJSONBean(String json){
        this.json = json;
        this.hasSynchronized = false;
    }

    public Boolean getHasSynchronized() {
        return hasSynchronized;
    }

    public String getJson() {
        return json;
    }

    public void setHasSynchronized(Boolean hasSynchronized) {
        this.hasSynchronized = hasSynchronized;
    }
}
