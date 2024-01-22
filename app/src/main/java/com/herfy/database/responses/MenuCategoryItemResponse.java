package com.herfy.database.responses;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MenuCategoryItemResponse implements Serializable {

    @SerializedName("id")
    private String id = "";

    @SerializedName("type")
    private String type = "";

    @SerializedName("attributes")
    private MenuCategoryItemAttributesResponse attributes = new MenuCategoryItemAttributesResponse();

    public MenuCategoryItemResponse() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MenuCategoryItemAttributesResponse getAttributes() {
        return attributes;
    }

    public void setAttributes(MenuCategoryItemAttributesResponse attributes) {
        this.attributes = attributes;
    }

}
