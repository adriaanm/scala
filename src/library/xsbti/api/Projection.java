package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Projection extends SimpleType
{
        public Projection(SimpleType prefix, String id)
        {
                this.prefix = prefix;
                this.id = id;
        }
        private final SimpleType prefix;
        private final String id;
        public final SimpleType prefix()
        {
                return prefix;
        }
        public final String id()
        {
                return id;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Projection)) {
                         return false;
                } else {
                        Projection o = (Projection)obj;
                        return prefix().equals(o.prefix()) && id().equals(o.id());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + prefix().hashCode()) + id().hashCode();
        }
        public String toString()
        {
                 return "Projection(" + "prefix: " + prefix() + ", " + "id: " + id() + ")";
        }
}
