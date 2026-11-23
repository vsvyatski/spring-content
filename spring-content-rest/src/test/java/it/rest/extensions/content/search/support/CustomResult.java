package it.rest.extensions.content.search.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;

public class CustomResult {

    @ContentId
    private String contentId;

    @Highlight
    private String highlight;

    @Attribute(name = "foo")
    private String foo;

    @Attribute(name = "bar")
    private String bar;

    public CustomResult(String contentId, String highlight, String foo, String bar) {
        this.contentId = contentId;
        this.highlight = highlight;
        this.foo = foo;
        this.bar = bar;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @SuppressWarnings("unused")
    public String getHighlight() {
        return highlight;
    }

    @SuppressWarnings("unused")
    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    @SuppressWarnings("unused")
    public String getBar() {
        return bar;
    }

    @SuppressWarnings("unused")
    public void setBar(String bar) {
        this.bar = bar;
    }
}