package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class AnnotationArgument implements java.io.Serializable
{
        public AnnotationArgument(String name, String value)
        {
                this.name = name;
                this.value = value;
        }
        private final String name;
        private final String value;
        public final String name()
        {
                return name;
        }
        public final String value()
        {
                return value;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof AnnotationArgument)) {
                         return false;
                } else {
                        AnnotationArgument o = (AnnotationArgument)obj;
                        return name().equals(o.name()) && value().equals(o.value());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + name().hashCode()) + value().hashCode();
        }
        public String toString()
        {
                 return "AnnotationArgument(" + "name: " + name() + ", " + "value: " + value() + ")";
        }
}
