package io.opentracing.contrib.spring.web.starter.properties;

import io.opentracing.contrib.spring.web.client.decorator.RestTemplateHeaderSpanDecorator.HeaderEntry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.Ordered;


public class HeaderProperties {

    private boolean enabled = true;
    /**
     * Span decorator order
     */
    private int order = Ordered.LOWEST_PRECEDENCE - 20000;
    private String baseTagKey = "http.header";
    private List<HeaderEntry> entries = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getOrder() {
      return order;
    }

    public void setOrder(int order) {
      this.order = order;
    }

    public String getBaseTagKey() {
      return baseTagKey;
    }

    public void setBaseTagKey(String baseTagKey) {
      this.baseTagKey = baseTagKey;
    }

  public List<HeaderEntry> getEntries() {
    return entries;
  }

  public void setEntries(
      List<HeaderEntry> entries) {
    this.entries = entries;
  }
}
