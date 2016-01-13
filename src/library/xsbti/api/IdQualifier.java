package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class IdQualifier extends Qualifier
{
        public IdQualifier(String value)
        {
                this.value = value;
        }
        private final String value;
        public final String value()
        {
                return value;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof IdQualifier)) {
                         return false;
                } else {
                        IdQualifier o = (IdQualifier)obj;
                        return value().equals(o.value());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + value().hashCode();
        }
        public String toString()
        {
                 return "IdQualifier(" + "value: " + value() + ")";
        }
}
