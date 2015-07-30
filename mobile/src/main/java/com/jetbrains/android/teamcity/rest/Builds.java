package com.jetbrains.android.teamcity.rest;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by breandan on 7/30/2015.
 */
public class Builds {

    public List<Build> build;

    public static class Build {
        String buildTypeId;
        String href;
        int id;
        int number;
        String state;
        String status;
        String webUrl;
    }

    @Override
    public String toString() {
        String s = "";
        for (Build build : this.build) {
            s = s + ", " + build.href + ":" + build.number;
        }

        return s;
    }

}
