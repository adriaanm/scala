package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class ParameterRef extends SimpleType
{
        public ParameterRef(String id)
        {
                this.id = id;
        }
        private final String id;
        public final String id()
        {
                return id;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof ParameterRef)) {
                         return false;
                } else {
                        ParameterRef o = (ParameterRef)obj;
                        return id().equals(o.id());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + id().hashCode();
        }
        public String toString()
        {
                 return "ParameterRef(" + "id: " + id() + ")";
        }
}
