package org.mvnsearch.jbang;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JBangSpringNativeIntegrationTest {

    @Test
    public void testExtractAotConfig() {
        List<String> comments = new ArrayList<>();
        comments.add("//AOT:CONFIG removeXmlSupport=true removeSpelSupport=false");
        comments.add("//AOT:CONFIG removeYamlSupport=false");
        final Map<String, String> config = JBangSpringNativeIntegration.extractAotConfig(comments);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

}
