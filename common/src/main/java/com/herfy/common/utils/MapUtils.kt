package com.herfy.common.utils

import com.google.android.gms.maps.model.LatLng

fun LatLng.staticMap(apiKey: String): String {
    return "https://maps.google.com/maps/api/staticmap?center=" + this.latitude + "," + this.longitude + "&zoom=15&size=600x300&sensor=false&key=" + apiKey
}

fun areaJson(): String {
    return "{\n" +
            "    \"cities\": [\n" +
            "        {\n" +
            "            \"id\": \"1\",\n" +
            "            \"name\": \"City 1\",\n" +
            "            \"cities\": [\n" +
            "                {\n" +
            "                    \"id\": \"1\",\n" +
            "                    \"name\": \"City 1 Area 1\",\n" +
            "                    \"lat\": 14.4210551,\n" +
            "                    \"long\": 121.148541\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"2\",\n" +
            "                    \"name\": \"City 1 Area 2\",\n" +
            "                    \"lat\": 14.6805194,\n" +
            "                    \"long\": 121.0216022\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"3\",\n" +
            "                    \"name\": \"City 1 Area 3\",\n" +
            "                    \"lat\": 14.6538301,\n" +
            "                    \"long\": 121.1350382\n" +
            "                }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"2\",\n" +
            "            \"name\": \"City 2\",\n" +
            "            \"cities\": [\n" +
            "                {\n" +
            "                    \"id\": \"1\",\n" +
            "                    \"name\": \"City 2 Area 1\",\n" +
            "                    \"lat\": 14.4210551,\n" +
            "                    \"long\": 121.148541\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"2\",\n" +
            "                    \"name\": \"City 2 Area 2\",\n" +
            "                    \"lat\": 14.6805194,\n" +
            "                    \"long\": 121.0216022\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"3\",\n" +
            "                    \"name\": \"City 2 Area 3\",\n" +
            "                    \"lat\": 14.6538301,\n" +
            "                    \"long\": 121.1350382\n" +
            "                }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"3\",\n" +
            "            \"name\": \"City 3\",\n" +
            "            \"cities\": [\n" +
            "                {\n" +
            "                    \"id\": \"1\",\n" +
            "                    \"name\": \"City 3 Area 1\",\n" +
            "                    \"lat\": 14.4210551,\n" +
            "                    \"long\": 121.148541\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"2\",\n" +
            "                    \"name\": \"City 3 Area 2\",\n" +
            "                    \"lat\": 14.6805194,\n" +
            "                    \"long\": 121.0216022\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"3\",\n" +
            "                    \"name\": \"City 3 Area 3\",\n" +
            "                    \"lat\": 14.6538301,\n" +
            "                    \"long\": 121.1350382\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}"
}