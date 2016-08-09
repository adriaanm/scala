// this compiles in javac, but gives an error when running scaladoc on it
public class Buildable {

  public static class Builder {}

  public static class InnerBuildable {
    public static class Builder {}
    public Builder foo() { return new Builder(); } // this line gives an error, that Builder is ambiguous
  }

}
