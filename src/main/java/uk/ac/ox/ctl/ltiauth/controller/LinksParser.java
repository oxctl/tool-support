package uk.ac.ox.ctl.ltiauth.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A very sloppy implementation of https://tools.ietf.org/html/rfc5988 for parsing relationship links.
 */
public class LinksParser {

    public enum Relation {CURRENT, NEXT, PREV, FIRST, LAST}

    public static class Link {
        private final String url;
        private final Relation relation;

        public Link(String url, Relation relation) {
            this.url = url;
            this.relation = relation;
        }

        public String getUrl() {
            return url;
        }

        public Relation getRelation() {
            return relation;
        }
    }

    private final Pattern linkPattern = Pattern.compile("<(?<link>.*)>");
    private final Pattern relationPattern = Pattern.compile(" *rel=\"(?<relation>.+)\"");

    public Link parseLink(String link) {
        // Technically you can have semicolons inside a quoted string.
        String[] split = link.split(";");
        if (split.length > 0) {
            Matcher linkMatcher = linkPattern.matcher(split[0]);
            if (linkMatcher.matches()) {
                String url = linkMatcher.group("link");
                for (int i = 1; i < split.length; i++) {
                    Matcher relationMatcher = relationPattern.matcher(split[i]);
                    if (relationMatcher.matches()) {
                        String relation = relationMatcher.group("relation");
                        return new Link(url, Relation.valueOf(relation.toUpperCase()));
                    }
                }
            }
        }
        return null;
    }

}
