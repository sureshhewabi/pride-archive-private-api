package uk.ac.ebi.pride.ws.pride.utils;

import uk.ac.ebi.pride.ws.pride.models.PrideArchiveAPIField;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 * <p>
 * This class helps to transfrom keywords from backend into properties in fronted for highligths and other fiters.
 *
 * <p>
 * Created by ypriverol (ypriverol@gmail.com) on 02/07/2018.
 */
public enum MappingsConstants {

    ACCESSION("fromTitle", "title");



    private String frontAPIKey;
    private String backendKey;

    MappingsConstants(String backendKey, String frontKey) {
        this.frontAPIKey = frontKey;
        this.backendKey = backendKey;
    }


    public static String backendKeyToAPIKey(String key) {
        for(MappingsConstants value: MappingsConstants.values()){
            if(value.backendKey.equalsIgnoreCase(key))
                return value.frontAPIKey;
        }
        return key;
    }
}
