package org.thoughtcrime.securesms.deltax.module;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {
  public String name;
  public String version;
  public String main;
  public String description;
  public String author;
  public DependsSpec depends;
  public ConflictsSpec conflicts;
  public List<String> provides;
  public boolean expose;

  public Manifest() {}

  public String getPackageName() {
    return author + "@" + name + ":" + version;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DependsSpec {
    public List<String> required;
    public List<String> optional;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ConflictsSpec {
    @JsonProperty("break")
    public List<String> breakList;

    public List<String> incompatible;
  }
}
