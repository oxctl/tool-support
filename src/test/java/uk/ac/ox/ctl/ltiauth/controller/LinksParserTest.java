package uk.ac.ox.ctl.ltiauth.controller;

import org.junit.jupiter.api.Test;
import uk.ac.ox.ctl.ltiauth.controller.LinksParser;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.ox.ctl.ltiauth.controller.LinksParser.Relation.NEXT;

class LinksParserTest {

    private LinksParser linksParser = new LinksParser();

    @Test
    public void testGoodLink() {
        LinksParser.Link link = linksParser.parseLink("<https://instance.instructure.com/api/lti/courses/123/names_and_roles?rlid=456&page=2&per_page=2>; rel=\"next\"");
        assertNotNull(link);
        assertEquals(NEXT, link.getRelation());
        assertEquals("https://instance.instructure.com/api/lti/courses/123/names_and_roles?rlid=456&page=2&per_page=2", link.getUrl());
    }

}