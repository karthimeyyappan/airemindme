package com.server.realsync.dto;

public class CustomerFieldSettingsResponse {
    private String field1Name;
    private String field2Name;
    private String field3Name;
    private String field4Name;
    private String field5Name;

    public CustomerFieldSettingsResponse() {}

    public CustomerFieldSettingsResponse(String field1Name, String field2Name, String field3Name, String field4Name, String field5Name) {
        this.field1Name = field1Name;
        this.field2Name = field2Name;
        this.field3Name = field3Name;
        this.field4Name = field4Name;
        this.field5Name = field5Name;
    }

    public String getField1Name() {
        return field1Name;
    }

    public void setField1Name(String field1Name) {
        this.field1Name = field1Name;
    }

    public String getField2Name() {
        return field2Name;
    }

    public void setField2Name(String field2Name) {
        this.field2Name = field2Name;
    }

    public String getField3Name() {
        return field3Name;
    }

    public void setField3Name(String field3Name) {
        this.field3Name = field3Name;
    }

    public String getField4Name() {
        return field4Name;
    }

    public void setField4Name(String field4Name) {
        this.field4Name = field4Name;
    }

    public String getField5Name() {
        return field5Name;
    }

    public void setField5Name(String field5Name) {
        this.field5Name = field5Name;
    }
}
